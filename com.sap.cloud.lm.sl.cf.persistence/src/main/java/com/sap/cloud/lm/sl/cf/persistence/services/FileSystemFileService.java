package com.sap.cloud.lm.sl.cf.persistence.services;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.sap.cloud.lm.sl.cf.persistence.DataSourceWithDialect;
import com.sap.cloud.lm.sl.cf.persistence.message.Messages;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.processors.FileDownloadProcessor;

public class FileSystemFileService extends AbstractFileService {

    private static final String DEFAULT_FILES_STORAGE_PATH = "files";

    private String storagePath;

    public FileSystemFileService(DataSourceWithDialect dataSourceWithDialect) {
        this(dataSourceWithDialect, DEFAULT_FILES_STORAGE_PATH);
    }

    public FileSystemFileService(DataSourceWithDialect dataSourceWithDialect, String storagePath) {
        super(DEFAULT_TABLE_NAME, dataSourceWithDialect);
        this.storagePath = storagePath;
    }

    @Override
    protected boolean storeFile(FileEntry fileEntry, InputStream inputStream) throws FileStorageException {
        storeFileContent(fileEntry, inputStream);
        return storeFileAttributes(fileEntry);
    }

    private void storeFileContent(FileEntry fileEntry, InputStream inputStream) throws FileStorageException {
        try {
            Path filesDirectory = getFilesDirectory(fileEntry.getSpace());
            Path newFilePath = Paths.get(filesDirectory.toString(), fileEntry.getId());
            logger.trace(MessageFormat.format(Messages.STORING_FILE_TO_PATH_0, newFilePath));
            Files.copy(inputStream, newFilePath, StandardCopyOption.REPLACE_EXISTING);
            File newFile = newFilePath.toFile();
            if (!newFile.exists()) {
                throw new FileStorageException(
                    MessageFormat.format(Messages.FILE_UPLOAD_FAILED, fileEntry.getName(), fileEntry.getNamespace()));
            }
            logger.debug(
                MessageFormat.format(Messages.STORED_FILE_0_WITH_SIZE_1_SUCCESSFULLY_2, newFile, newFile.length()));
        } catch (IOException e) {
            throw new FileStorageException(MessageFormat.format(Messages.FILE_UPLOAD_FAILED, fileEntry.getName(), fileEntry.getNamespace()), e);
        }
    }

    @Override
    protected void deleteFileContent(String space, String id) throws FileStorageException {
        try {
            Path filesDirectory = getFilesDirectory(space);
            Path filePath = Paths.get(filesDirectory.toString(), id);
            logger.debug(MessageFormat.format(Messages.DELETING_FILE_WITH_PATH_0, filePath.toString()));
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new FileStorageException(MessageFormat.format(Messages.ERROR_DELETING_FILE_WITH_ID, id), e);
        }
    }

    @Override
    public void processFileContent(FileDownloadProcessor fileDownloadProcessor) throws FileStorageException {
        FileEntry fileEntry = fileDownloadProcessor.getFileEntry();
        if (deleteOrphanedFileAttributes(fileEntry)) {
            throw new FileStorageException(
                MessageFormat.format(Messages.FILE_WITH_ID_AND_SPACE_DOES_NOT_EXIST, fileEntry.getId(), fileEntry.getSpace()));
        }
        InputStream fileContentStream = null;
        try {
            Path filePathLocation = getFilePath(fileDownloadProcessor.getFileEntry());
            logger.trace(MessageFormat.format(Messages.PROCESSING_FILE_0, filePathLocation));
            fileContentStream = Files.newInputStream(filePathLocation);
            fileDownloadProcessor.processContent(fileContentStream);
        } catch (Exception e) {
            throw new FileStorageException(e);
        } finally {
            if (fileContentStream != null) {
                IOUtils.closeQuietly(fileContentStream);
            }
        }
    }

    private Path getFilePath(FileEntry entry) throws IOException {
        Path filesDirectory = getFilesDirectory(entry.getSpace());
        return Paths.get(filesDirectory.toString(), entry.getId());
    }

    @Override
    public int deleteAll(String space, String namespace) throws FileStorageException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteBySpace(String space) throws FileStorageException {
        File spaceDirectory = getSpaceDirectory(space).toFile();
        try {
            if (spaceDirectory.exists()) {
                FileUtils.deleteDirectory(spaceDirectory);
            }
        } catch (IOException e) {
            throw new FileStorageException(MessageFormat.format(Messages.ERROR_DELETING_DIRECTORY, spaceDirectory), e);
        }
    }

    private Path getFilesDirectory(String space) throws IOException {
        Path filesPerSpaceDirectory = getFilesPerSpaceDirectory(space);
        Files.createDirectories(filesPerSpaceDirectory);
        return filesPerSpaceDirectory;
    }

    @Override
    public int deleteModifiedBefore(Date modificationTime) throws FileStorageException {
        super.deleteModifiedBefore(modificationTime);

        AtomicInteger deletedFiles = new AtomicInteger();
        final FileTime modificationTimeUpperBound = FileTime.fromMillis(modificationTime.getTime());
        try {

            Files.walkFileTree(Paths.get(storagePath), new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (attrs.lastModifiedTime()
                        .compareTo(modificationTimeUpperBound) < 0) {
                        logger.trace(MessageFormat.format(Messages.DELETING_FILE_WITH_PATH_0, file.toString()));
                        boolean deleted = Files.deleteIfExists(file);
                        logger.debug(MessageFormat.format(Messages.DELETED_FILE_0_SUCCESSFULLY_1, file.toString(), deleted));
                        if (deleted) {
                            deletedFiles.incrementAndGet();
                        }
                        return FileVisitResult.CONTINUE;
                    }
                    return super.visitFile(file, attrs);
                }
            });
        } catch (IOException e) {
            throw new FileStorageException(e.getMessage(), e);
        }
        return deletedFiles.intValue();
    }

    private Path getFilesPerSpaceDirectory(String space) {
        return Paths.get(storagePath, space, DEFAULT_FILES_STORAGE_PATH);
    }

    @Override
    public List<FileEntry> listFiles(String space, String namespace) throws FileStorageException {
        List<FileEntry> allEntries = super.listFiles(space, namespace);
        return deleteOrphanedFileAttributes(allEntries);
    }

    @Override
    public FileEntry getFile(String space, String id) throws FileStorageException {
        FileEntry entry = super.getFile(space, id);
        if (entry == null) {
            return null;
        }
        return deleteOrphanedFileAttributes(entry) ? null : entry;
    }

    private List<FileEntry> deleteOrphanedFileAttributes(List<FileEntry> entries) throws FileStorageException {
        List<FileEntry> entriesWithContent = new ArrayList<>();
        for (FileEntry entry : entries) {
            if (!deleteOrphanedFileAttributes(entry)) {
                entriesWithContent.add(entry);
            }
        }
        return entriesWithContent;
    }

    private boolean deleteOrphanedFileAttributes(FileEntry entry) throws FileStorageException {
        boolean shouldDelete = !hasContent(entry);
        if (shouldDelete) {
            deleteFileAttributes(entry.getSpace(), entry.getId());
        }
        return shouldDelete;
    }

    private boolean hasContent(FileEntry entry) throws FileStorageException {
        try {
            Path filePath = getFilePath(entry);
            return filePath.toFile()
                .exists();// squid:S3725 - java 8 Files.exists() has poor performance
        } catch (IOException e) {
            throw new FileStorageException(e.getMessage(), e);
        }
    }

    public String getStoragePath() {
        return storagePath;
    }

    private Path getSpaceDirectory(String space) {
        return Paths.get(storagePath, space);
    }

}
