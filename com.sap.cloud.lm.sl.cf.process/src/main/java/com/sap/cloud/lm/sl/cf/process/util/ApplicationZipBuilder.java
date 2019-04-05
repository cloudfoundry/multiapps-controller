package com.sap.cloud.lm.sl.cf.process.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.cloudfoundry.client.lib.io.UtcAdjustedZipEntry;

import com.sap.cloud.lm.sl.cf.core.util.FileUtils;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

public class ApplicationZipBuilder {
    private static final int BUFFER_SIZE = 4 * 1024; // 4KB
    private ApplicationArchiveReader archiveReader;
    private String moduleFileName;
    private StepLogger logger;
    private Set<String> alreadyUploadedFiles;

    public ApplicationZipBuilder(ApplicationArchiveReader archiveReader, String moduleFileName, StepLogger logger,
        Set<String> alreadyUploadedFiles) {
        this.archiveReader = archiveReader;
        this.moduleFileName = moduleFileName;
        this.logger = logger;
        this.alreadyUploadedFiles = alreadyUploadedFiles;
    }

    public Path extractApplicationInNewArchive() {
        Path appPath = null;
        try {
            appPath = createTempFile();
            saveAllEntries(appPath);
            return appPath;
        } catch (Exception e) {
            cleanUp(appPath);
            throw new SLException(e, Messages.ERROR_RETRIEVING_MTA_MODULE_CONTENT, moduleFileName);
        }
    }

    private void saveAllEntries(Path dirPath) throws IOException {
        try (OutputStream fileOutputStream = Files.newOutputStream(dirPath)) {
            if (!isFile(moduleFileName)) {
                saveAsZip(fileOutputStream);
            } else {
                saveToFile(fileOutputStream);
            }
        }
    }

    private void saveAsZip(OutputStream fileOutputStream) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {
            ZipEntry zipEntry = archiveReader.getFirstZipEntry();
            do {
                if (!isAlreadyUploaded(zipEntry.getName()) && isFile(zipEntry.getName())) {
                    zipOutputStream.putNextEntry(createNewZipEntry(zipEntry.getName()));
                    copy(archiveReader.getZipEntryStream(), zipOutputStream);
                    zipOutputStream.closeEntry();

                }
            } while ((zipEntry = archiveReader.getNextEntryByName(moduleFileName)) != null);
        }
    }

    private ZipEntry createNewZipEntry(String zipEntryName) {
        return new UtcAdjustedZipEntry(getRelativePathOfZipEntry(zipEntryName));
    }

    protected String getRelativePathOfZipEntry(String zipEntryName) {
        return Paths.get(moduleFileName)
            .relativize(Paths.get(zipEntryName))
            .toString();
    }

    private void saveToFile(OutputStream fileOutputStream) throws IOException {
        ZipEntry zipEntry = archiveReader.getFirstZipEntry();
        do {
            if (!isAlreadyUploaded(zipEntry.getName())) {
                copy(archiveReader.getZipEntryStream(), fileOutputStream);
            }
        } while ((zipEntry = archiveReader.getNextEntryByName(moduleFileName)) != null);
    }

    private boolean isAlreadyUploaded(String zipEntryName) {
        return alreadyUploadedFiles.contains(zipEntryName);
    }

    protected void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int numberOfReadBytes = 0;
        while ((numberOfReadBytes = input.read(buffer)) != -1) {
            output.write(buffer, 0, numberOfReadBytes);
        }
    }

    protected Path createTempFile() throws IOException {
        return Files.createTempFile(null, getFileExtension());
    }

    private String getFileExtension() {
        return FilenameUtils.EXTENSION_SEPARATOR_STR + "zip";
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
