package org.cloudfoundry.multiapps.controller.process.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.FilenameUtils;
import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.util.FileUtils;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
public class ApplicationZipBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationZipBuilder.class);
    private static final int BUFFER_SIZE = 4 * 1024; // 4KB

    private final FileService fileService;
    private final ApplicationArchiveIterator applicationArchiveIterator;
    private final ArchiveEntryExtractor archiveEntryExtractor;

    @Inject
    public ApplicationZipBuilder(FileService fileService, ApplicationArchiveIterator applicationArchiveIterator,
                                 ArchiveEntryExtractor archiveEntryExtractor) {
        this.fileService = fileService;
        this.applicationArchiveIterator = applicationArchiveIterator;
        this.archiveEntryExtractor = archiveEntryExtractor;
    }

    public Path extractApplicationInNewArchive(ApplicationArchiveContext applicationArchiveContext) {
        Path appPath = null;
        try {
            appPath = createTempFile();
            if (ArchiveEntryExtractorUtil.containsDirectory(applicationArchiveContext.getModuleFileName(),
                                                            applicationArchiveContext.getArchiveEntryWithStreamPositions())) {
                LOGGER.info(MessageFormat.format(Messages.MODULE_0_CONTENT_IS_A_DIRECTORY, applicationArchiveContext.getModuleFileName()));
                extractDirectoryContent(applicationArchiveContext, appPath);
            } else {
                extractModuleContent(applicationArchiveContext, appPath);
            }
            return appPath;
        } catch (Exception e) {
            FileUtils.cleanUp(appPath, LOGGER);
            throw new SLException(e, Messages.ERROR_RETRIEVING_MTA_MODULE_CONTENT, applicationArchiveContext.getModuleFileName());
        }
    }

    protected Path createTempFile() {
        try {
            return Files.createTempFile(null, getFileExtension());
        } catch (IOException e) {
            throw new SLException(e, e.getMessage());
        }
    }

    private String getFileExtension() {
        return FilenameUtils.EXTENSION_SEPARATOR_STR + "zip";
    }

    private void extractDirectoryContent(ApplicationArchiveContext applicationArchiveContext, Path applicationPath)
        throws FileStorageException {
        fileService.consumeFileContent(applicationArchiveContext.getSpaceId(), applicationArchiveContext.getAppArchiveId(),
                                       archiveStream -> {
                                           try (ZipArchiveInputStream zipArchiveInputStream = new ZipArchiveInputStream(archiveStream)) {
                                               saveAllEntries(applicationPath, applicationArchiveContext, zipArchiveInputStream);
                                           }
                                       });
    }

    private void saveAllEntries(Path dirPath, ApplicationArchiveContext applicationArchiveContext,
                                ZipArchiveInputStream zipArchiveInputStream)
        throws IOException {
        try (OutputStream fileOutputStream = Files.newOutputStream(dirPath)) {
            ZipEntry zipEntry = applicationArchiveIterator.getFirstZipEntry(applicationArchiveContext.getModuleFileName(),
                                                                            zipArchiveInputStream);
            if (zipEntry.isDirectory()) {
                saveAsZip(fileOutputStream, applicationArchiveContext, zipEntry, zipArchiveInputStream);
            } else {
                saveToFile(fileOutputStream, applicationArchiveContext, zipEntry, zipArchiveInputStream);
            }
        }
    }

    private void saveAsZip(OutputStream fileOutputStream, ApplicationArchiveContext applicationArchiveContext, ZipEntry zipEntry,
                           ZipArchiveInputStream zipArchiveInputStream)
        throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {
            String moduleFileName = applicationArchiveContext.getModuleFileName();
            do {
                if (!isAlreadyUploaded(zipEntry.getName(), applicationArchiveContext) && !zipEntry.isDirectory()) {
                    zipOutputStream.putNextEntry(createNewZipEntry(zipEntry.getName(), moduleFileName));
                    copy(zipArchiveInputStream, zipOutputStream, applicationArchiveContext);
                    zipOutputStream.closeEntry();
                }
            } while ((zipEntry = applicationArchiveIterator.getNextEntryByName(moduleFileName, zipArchiveInputStream)) != null);
        }
    }

    private boolean isAlreadyUploaded(String zipEntryName, ApplicationArchiveContext applicationArchiveContext) {
        return applicationArchiveContext.getAlreadyUploadedFiles()
                                        .contains(zipEntryName);
    }

    private ZipEntry createNewZipEntry(String zipEntryName, String moduleFileName) {
        return new ZipEntry(FileUtils.getRelativePath(moduleFileName, zipEntryName));
    }

    protected void copy(InputStream input, OutputStream output, ApplicationArchiveContext applicationArchiveContext) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int numberOfReadBytes = 0;
        long maxSizeInBytes = applicationArchiveContext.getMaxSizeInBytes();
        while ((numberOfReadBytes = input.read(buffer)) != -1) {
            long currentSizeInBytes = applicationArchiveContext.getCurrentSizeInBytes();
            if (currentSizeInBytes + numberOfReadBytes > maxSizeInBytes) {
                throw new ContentException(org.cloudfoundry.multiapps.mta.Messages.ERROR_SIZE_OF_FILE_EXCEEDS_CONFIGURED_MAX_SIZE_LIMIT,
                                           currentSizeInBytes + numberOfReadBytes,
                                           applicationArchiveContext.getModuleFileName(),
                                           maxSizeInBytes);
            }
            output.write(buffer, 0, numberOfReadBytes);
            applicationArchiveContext.calculateCurrentSizeInBytes(numberOfReadBytes);
        }
    }

    private void saveToFile(OutputStream fileOutputStream, ApplicationArchiveContext applicationArchiveContext, ZipEntry zipEntry,
                            ZipArchiveInputStream zipArchiveInputStream)
        throws IOException {
        String moduleFileName = applicationArchiveContext.getModuleFileName();
        do {
            if (!isAlreadyUploaded(zipEntry.getName(), applicationArchiveContext)) {
                copy(zipArchiveInputStream, fileOutputStream, applicationArchiveContext);
            }
        } while ((zipEntry = applicationArchiveIterator.getNextEntryByName(moduleFileName, zipArchiveInputStream)) != null);
    }

    private void extractModuleContent(ApplicationArchiveContext applicationArchiveContext, Path appPath) throws IOException {
        try (OutputStream fileOutputStream = Files.newOutputStream(appPath)) {
            ArchiveEntryWithStreamPositions archiveEntryWithStreamPositions = ArchiveEntryExtractorUtil.findEntry(
                applicationArchiveContext.getModuleFileName(),
                applicationArchiveContext.getArchiveEntryWithStreamPositions());
            archiveEntryExtractor.processFileEntryBytes(ImmutableFileEntryProperties.builder()
                                                                                    .guid(applicationArchiveContext.getAppArchiveId())
                                                                                    .name(archiveEntryWithStreamPositions.getName())
                                                                                    .spaceGuid(applicationArchiveContext.getSpaceId())
                                                                                    .maxFileSizeInBytes(
                                                                                        applicationArchiveContext.getMaxSizeInBytes())
                                                                                    .build(),
                                                        archiveEntryWithStreamPositions,
                                                        (bytesBuffer, bytesRead) -> writeModuleContent(bytesBuffer, bytesRead,
                                                                                                       fileOutputStream));
        }
    }

    private void writeModuleContent(byte[] bytesBuffer, Integer bytesRead, OutputStream fileOutputStream) {
        try {
            fileOutputStream.write(bytesBuffer, 0, bytesRead);
        } catch (IOException e) {
            throw new SLException(e, e.getMessage());
        }
    }

}
