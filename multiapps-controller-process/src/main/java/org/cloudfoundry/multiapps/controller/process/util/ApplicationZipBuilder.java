package org.cloudfoundry.multiapps.controller.process.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.io.FilenameUtils;
import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.util.FileUtils;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.stream.ArchiveEntryWithStreamPositions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ApplicationZipBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationZipBuilder.class);
    private static final int BUFFER_SIZE = 4 * 1024; // 4KB

    private final FileService fileService;
    private final ApplicationArchiveReader applicationArchiveReader;

    @Inject
    public ApplicationZipBuilder(FileService fileService, ApplicationArchiveReader applicationArchiveReader) {
        this.fileService = fileService;
        this.applicationArchiveReader = applicationArchiveReader;
    }

    public Path extractApplicationInNewArchive(ApplicationArchiveContext applicationArchiveContext) {
        Path appPath = null;
        try {
            final Path applicationPath = createTempFile();
            appPath = applicationPath;
            List<ArchiveEntryWithStreamPositions> archiveEntriesWithStreamPositions = applicationArchiveContext.getArchiveEntryWithStreamPositions()
                                                                                                               .stream()
                                                                                                               .filter(e -> e.getName()
                                                                                                                             .startsWith(applicationArchiveContext.getModuleFileName()))
                                                                                                               .toList();
            Optional<ArchiveEntryWithStreamPositions> directoryEntry = archiveEntriesWithStreamPositions.stream()
                                                                                                        .filter(ArchiveEntryWithStreamPositions::isDirectory)
                                                                                                        .findFirst();
            if (directoryEntry.isPresent()) {
                System.out.println("DIRECTORY FOUND FOR APP DIGEST CALC");
                fileService.processFileContent(applicationArchiveContext.getSpaceId(), applicationArchiveContext.getAppArchiveId(),
                                               archiveStream -> {
                                                   try (ZipInputStream zipArchiveInputStream = new ZipInputStream(archiveStream)) {
                                                       saveAllEntries(applicationPath, applicationArchiveContext, zipArchiveInputStream);
                                                       return null;
                                                   }
                                               });
            } else {
                System.out.println("UNGA BUNGATA ");
                try (FileOutputStream fileOutputStream = new FileOutputStream(appPath.toFile())) {
                    for (ArchiveEntryWithStreamPositions archiveEntryWithStreamPositions : archiveEntriesWithStreamPositions) {
                        fileService.processFileContentWithOffset(applicationArchiveContext.getSpaceId(),
                                                                 applicationArchiveContext.getAppArchiveId(), entryStream -> {
                                                                     if (archiveEntryWithStreamPositions.getCompressionMethod() == 0) {
                                                                         copy(entryStream, fileOutputStream, applicationArchiveContext);
                                                                     } else {
                                                                         InflatorUtil.inflate(entryStream, fileOutputStream,
                                                                                              applicationArchiveContext);
                                                                     }
                                                                     return null;
                                                                 }, archiveEntryWithStreamPositions.getStartPosition(),
                                                                 archiveEntryWithStreamPositions.getEndPosition());
                    }
                }
            }

            return appPath;
        } catch (Exception e) {
            FileUtils.cleanUp(appPath, LOGGER);
            throw new SLException(e, Messages.ERROR_RETRIEVING_MTA_MODULE_CONTENT, applicationArchiveContext.getModuleFileName());
        }
    }

    protected void copy(InputStream input, OutputStream output, ApplicationArchiveContext applicationArchiveContext) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int numberOfReadBytes = 0;
        long maxSizeInBytes = applicationArchiveContext.getMaxSizeInBytes();
        while ((numberOfReadBytes = input.read(buffer)) != -1) {
            long currentSizeInBytes = applicationArchiveContext.getCurrentSizeInBytes();
            if (currentSizeInBytes + numberOfReadBytes > maxSizeInBytes) {
                throw new ContentException(Messages.SIZE_OF_APP_EXCEEDS_MAX_SIZE_LIMIT, maxSizeInBytes);
            }
            output.write(buffer, 0, numberOfReadBytes);
            applicationArchiveContext.calculateCurrentSizeInBytes(numberOfReadBytes);
        }
    }

    private ZipEntry createNewZipEntry(String zipEntryName, String moduleFileName) {
        return new UtcAdjustedZipEntry(FileUtils.getRelativePath(moduleFileName, zipEntryName));
    }

    private boolean isAlreadyUploaded(String zipEntryName, ApplicationArchiveContext applicationArchiveContext) {
        return applicationArchiveContext.getAlreadyUploadedFiles()
                                        .contains(zipEntryName);
    }

    private void saveAllEntries(Path dirPath, ApplicationArchiveContext applicationArchiveContext, ZipInputStream zipArchiveInputStream)
        throws IOException {
        try (OutputStream fileOutputStream = Files.newOutputStream(dirPath)) {
            ZipEntry zipEntry = applicationArchiveReader.getFirstZipEntry(applicationArchiveContext, zipArchiveInputStream);
            if (zipEntry.isDirectory()) {
                saveAsZip(fileOutputStream, applicationArchiveContext, zipEntry, zipArchiveInputStream);
            } else {
                saveToFile(fileOutputStream, applicationArchiveContext, zipEntry, zipArchiveInputStream);
            }
        }
    }

    private void saveToFile(OutputStream fileOutputStream, ApplicationArchiveContext applicationArchiveContext, ZipEntry zipEntry,
                            ZipInputStream zipArchiveInputStream)
        throws IOException {
        String moduleFileName = applicationArchiveContext.getModuleFileName();
        do {
            if (!isAlreadyUploaded(zipEntry.getName(), applicationArchiveContext)) {
                copy(zipArchiveInputStream, fileOutputStream, applicationArchiveContext);
            }
        } while ((zipEntry = applicationArchiveReader.getNextEntryByName(moduleFileName, zipArchiveInputStream)) != null);
    }

    private void saveAsZip(OutputStream fileOutputStream, ApplicationArchiveContext applicationArchiveContext, ZipEntry zipEntry,
                           ZipInputStream zipArchiveInputStream)
        throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {
            String moduleFileName = applicationArchiveContext.getModuleFileName();
            do {
                if (!isAlreadyUploaded(zipEntry.getName(), applicationArchiveContext) && !zipEntry.isDirectory()) {
                    zipOutputStream.putNextEntry(createNewZipEntry(zipEntry.getName(), moduleFileName));
                    copy(zipArchiveInputStream, zipOutputStream, applicationArchiveContext);
                    zipOutputStream.closeEntry();

                }
            } while ((zipEntry = applicationArchiveReader.getNextEntryByName(moduleFileName, zipArchiveInputStream)) != null);
        }
    }

    protected Path createTempFile() throws IOException {
        return Files.createTempFile(null, getFileExtension());
    }

    private String getFileExtension() {
        return FilenameUtils.EXTENSION_SEPARATOR_STR + "zip";
    }

}
