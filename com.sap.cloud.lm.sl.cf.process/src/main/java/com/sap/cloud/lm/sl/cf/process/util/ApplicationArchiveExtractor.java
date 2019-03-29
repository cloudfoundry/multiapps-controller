package com.sap.cloud.lm.sl.cf.process.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FilenameUtils;
import org.cloudfoundry.client.lib.domain.CloudResource;
import org.cloudfoundry.client.lib.io.UtcAdjustedZipEntry;

import com.sap.cloud.lm.sl.cf.core.util.FileUtils;
import com.sap.cloud.lm.sl.cf.persistence.services.FileUploader;
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
    private MessageDigest applicationDigest;
    private Set<String> knownFileNames;

    public ApplicationArchiveExtractor(InputStream inputStream, String moduleFileName, long maxSizeInBytes, Set<String> knownFileNames,
        StepLogger logger) {
        this.inputStream = new ZipInputStream(inputStream);
        this.moduleFileName = moduleFileName;
        this.maxSizeInBytes = maxSizeInBytes;
        this.knownFileNames = knownFileNames;
        this.logger = logger;
    }

    public Collection<CloudResource> getApplicationMetaData() {
        try {
            moveStreamToApplicationEntry();
            return collectApplicationMetadata();
        } catch (Exception e) {
            throw new SLException(e, Messages.ERROR_RETRIEVING_MTA_MODULE_CONTENT, moduleFileName);
        }
    }

    private Collection<CloudResource> collectApplicationMetadata() throws IOException, NoSuchAlgorithmException, DigestException {
        this.applicationDigest = MessageDigest.getInstance(FileUploader.DIGEST_METHOD);
        Collection<CloudResource> cloudResourceCollection = new ArrayList<>();
        do {
            if (isFile(zipEntryName)) {
                String zipEntryDigest = getDigestOfZipEntry(inputStream, "SHA");
                cloudResourceCollection.add(new CloudResource(zipEntryName, currentSizeInBytes, zipEntryDigest));
            }
        } while (getNextEntryByName(moduleFileName) != null);
        return cloudResourceCollection;
    }

    public Path extractApplicationInNewArchive() {
        Path appPath = null;
        try {
            moveStreamToApplicationEntry();
            appPath = createTempFile();
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

    private void saveAllEntries(Path dirPath) throws IOException {
        try (OutputStream fileOutputStream = Files.newOutputStream(dirPath)) {
            if (isDirectory(moduleFileName)) {
                saveAsZip(fileOutputStream);
            } else {
                saveToFile(fileOutputStream);
            }
        }
    }

    private void saveAsZip(OutputStream fileOutputStream) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {
            do {
                if (!hasEntryInCache() && isFile(zipEntryName)) {
                    zipOutputStream.putNextEntry(createZipEntry());
                    copy(inputStream, zipOutputStream);
                    zipOutputStream.closeEntry();

                }
            } while (getNextEntryByName(moduleFileName) != null);
        }
    }

    private ZipEntry createZipEntry() {
        return new UtcAdjustedZipEntry(getRelativePathOfZipEntry());
    }

    private String getRelativePathOfZipEntry() {
        return Paths.get(moduleFileName)
            .relativize(Paths.get(zipEntryName))
            .toString();
    }

    private void saveToFile(OutputStream fileOutputStream) throws IOException {
        do {
            if (!hasEntryInCache()) {
                copy(inputStream, fileOutputStream);
            }
        } while (getNextEntryByName(moduleFileName) != null);
    }

    private boolean hasEntryInCache() {
        return knownFileNames.contains(zipEntryName);
    }

    private String getDigestOfZipEntry(InputStream input, String algorithm) throws DigestException, IOException, NoSuchAlgorithmException {
        MessageDigest zipEntryMessageDigest = MessageDigest.getInstance(algorithm);
        byte[] buffer = new byte[BUFFER_SIZE];
        int numberOfReadBytes = 0;
        while ((numberOfReadBytes = input.read(buffer)) != -1) {
            if (currentSizeInBytes + numberOfReadBytes > maxSizeInBytes) {
                throw new ContentException(Messages.SIZE_OF_APP_EXCEEDS_MAX_SIZE_LIMIT, maxSizeInBytes);
            }
            zipEntryMessageDigest.update(buffer, 0, numberOfReadBytes);
            applicationDigest.update(buffer, 0, numberOfReadBytes);
            currentSizeInBytes += numberOfReadBytes;
        }
        return DatatypeConverter.printHexBinary(zipEntryMessageDigest.digest());
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

    public String getApplicationDigest() {
        return DatatypeConverter.printHexBinary(applicationDigest.digest());
    }

    protected void validateEntry(ZipEntry entry) {
        FileUtils.validatePath(entry.getName());
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

    private boolean isDirectory(String fileName) {
        return FileUtils.isDirectory(fileName);
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