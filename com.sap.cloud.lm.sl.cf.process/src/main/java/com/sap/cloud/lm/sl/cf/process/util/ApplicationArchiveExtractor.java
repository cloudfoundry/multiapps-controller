package com.sap.cloud.lm.sl.cf.process.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FilenameUtils;

import com.sap.cloud.lm.sl.cf.core.util.FileUtils;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.SLException;

public class ApplicationArchiveExtractor {

    private static final int BUFFER_SIZE = 4 * 1024; // 4KB
    private ZipInputStream inputStream;
    private String zipEntryName;
    private StepLogger logger;
    private String moduleFileName;
    private long maxSizeInBytes;
    private long currentSizeInBytes;

    public ApplicationArchiveExtractor(InputStream inputStream, String moduleFileName, long maxSizeInBytes, StepLogger logger) {
        this.inputStream = new ZipInputStream(inputStream);
        this.moduleFileName = moduleFileName;
        this.maxSizeInBytes = maxSizeInBytes;
        this.logger = logger;
    }

    public Path extract() {
        Path appPath = null;
        try {
            moveStreamToApplicationEntry();

            if (isFile(moduleFileName)) {
                appPath = createTempFile();
                saveEntry(appPath);
                return appPath;
            }

            appPath = createTempDirectory();
            saveAllEntries(appPath);
            return appPath;
        } catch (Exception e) {
            cleanUp(appPath);
            throw new SLException(e, Messages.ERROR_RETRIEVING_MTA_MODULE_CONTENT, moduleFileName);
        }
    }

    private void moveStreamToApplicationEntry() throws IOException {
        if (getNextEntryByName(moduleFileName) == null) {
            throw new ContentException(com.sap.cloud.lm.sl.mta.message.Messages.CANNOT_FIND_ARCHIVE_ENTRY, moduleFileName);
        }
    }

    private void saveEntry(Path filePath) throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(filePath)) {
            copy(inputStream, outputStream);
        }
    }

    private void saveAllEntries(Path dirPath) throws IOException {
        do {
            if (isFile(zipEntryName)) {
                Path filePath = resolveEntryPath(dirPath);
                Files.createDirectories(filePath.getParent());
                filePath = Files.createFile(filePath);
                saveEntry(filePath);
            }
        } while (getNextEntryByName(moduleFileName) != null);
    }

    private void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int numberOfReadBytes = 0;
        while ((numberOfReadBytes = input.read(buffer)) != -1) {
            if (currentSizeInBytes + numberOfReadBytes > maxSizeInBytes) {
                throw new ContentException(Messages.SIZE_OF_APP_EXCEEDS_MAX_SIZE_LIMIT, maxSizeInBytes);
            }
            output.write(buffer, 0, numberOfReadBytes);
            currentSizeInBytes += numberOfReadBytes;
        }
    }

    public InputStream getNextEntryByName(String name) throws IOException {
        for (ZipEntry zipEntry; (zipEntry = inputStream.getNextEntry()) != null;) {
            if (zipEntry.getName()
                .startsWith(name)) {
                validateEntry(zipEntry);
                zipEntryName = zipEntry.getName();
                return inputStream;
            }
        }
        return null;
    }

    protected void validateEntry(ZipEntry entry) {
        FileUtils.validatePath(entry.getName());
    }

    private Path resolveEntryPath(Path dirPath) {
        return dirPath.resolve(zipEntryName.substring(moduleFileName.length()));
    }

    private Path createTempFile() throws IOException {
        return Files.createTempFile(null, getFileExtension());
    }

    private Path createTempDirectory() throws IOException {
        return Files.createTempDirectory(null);
    }
    
    private String getFileExtension() {
        String extension = FilenameUtils.getExtension(moduleFileName);
        return extension.isEmpty() ? extension : FilenameUtils.EXTENSION_SEPARATOR_STR + extension;
    }

    private boolean isFile(String fileName) {
        return !FileUtils.isDirectory(fileName);
    }

    protected void cleanUp(Path appPath) {
        if (appPath == null || !Files.exists(appPath)) {
            return;
        }

        try {
            logger.debug(Messages.DELETING_TEMP_FILE, appPath);
            org.apache.commons.io.FileUtils.forceDelete(appPath.toFile());
        } catch (IOException e) {
            logger.warn(Messages.ERROR_DELETING_APP_TEMP_FILE, appPath.toAbsolutePath());
        }
    }
}