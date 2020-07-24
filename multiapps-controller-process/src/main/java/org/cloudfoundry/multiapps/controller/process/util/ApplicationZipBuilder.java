package org.cloudfoundry.multiapps.controller.process.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.FilenameUtils;
import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.util.FileUtils;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ApplicationZipBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationZipBuilder.class);
    private static final int BUFFER_SIZE = 4 * 1024; // 4KB
    private final ApplicationArchiveReader applicationArchiveReader;

    @Inject
    public ApplicationZipBuilder(ApplicationArchiveReader applicationArchiveReader) {
        this.applicationArchiveReader = applicationArchiveReader;
    }

    public Path extractApplicationInNewArchive(ApplicationArchiveContext applicationArchiveContext) {
        Path appPath = null;
        try {
            appPath = createTempFile();
            saveAllEntries(appPath, applicationArchiveContext);
            return appPath;
        } catch (Exception e) {
            FileUtils.cleanUp(appPath, LOGGER);
            throw new SLException(e, Messages.ERROR_RETRIEVING_MTA_MODULE_CONTENT, applicationArchiveContext.getModuleFileName());
        }
    }

    private void saveAllEntries(Path dirPath, ApplicationArchiveContext applicationArchiveContext) throws IOException {
        try (OutputStream fileOutputStream = Files.newOutputStream(dirPath)) {
            ZipEntry zipEntry = applicationArchiveReader.getFirstZipEntry(applicationArchiveContext);
            if (zipEntry.isDirectory()) {
                saveAsZip(fileOutputStream, applicationArchiveContext, zipEntry);
            } else {
                saveToFile(fileOutputStream, applicationArchiveContext, zipEntry);
            }
        }
    }

    private void saveAsZip(OutputStream fileOutputStream, ApplicationArchiveContext applicationArchiveContext, ZipEntry zipEntry)
        throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {
            String moduleFileName = applicationArchiveContext.getModuleFileName();
            do {
                if (!isAlreadyUploaded(zipEntry.getName(), applicationArchiveContext) && !zipEntry.isDirectory()) {
                    zipOutputStream.putNextEntry(createNewZipEntry(zipEntry.getName(), moduleFileName));
                    copy(applicationArchiveContext.getZipInputStream(), zipOutputStream, applicationArchiveContext);
                    zipOutputStream.closeEntry();

                }
            } while ((zipEntry = applicationArchiveReader.getNextEntryByName(moduleFileName, applicationArchiveContext)) != null);
        }
    }

    private ZipEntry createNewZipEntry(String zipEntryName, String moduleFileName) {
        return new UtcAdjustedZipEntry(FileUtils.getRelativePath(moduleFileName, zipEntryName));
    }

    private void saveToFile(OutputStream fileOutputStream, ApplicationArchiveContext applicationArchiveContext, ZipEntry zipEntry)
        throws IOException {
        String moduleFileName = applicationArchiveContext.getModuleFileName();
        do {
            if (!isAlreadyUploaded(zipEntry.getName(), applicationArchiveContext)) {
                copy(applicationArchiveContext.getZipInputStream(), fileOutputStream, applicationArchiveContext);
            }
        } while ((zipEntry = applicationArchiveReader.getNextEntryByName(moduleFileName, applicationArchiveContext)) != null);
    }

    private boolean isAlreadyUploaded(String zipEntryName, ApplicationArchiveContext applicationArchiveContext) {
        return applicationArchiveContext.getAlreadyUploadedFiles()
                                        .contains(zipEntryName);
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

    protected Path createTempFile() throws IOException {
        return Files.createTempFile(null, getFileExtension());
    }

    private String getFileExtension() {
        return FilenameUtils.EXTENSION_SEPARATOR_STR + "zip";
    }

}
