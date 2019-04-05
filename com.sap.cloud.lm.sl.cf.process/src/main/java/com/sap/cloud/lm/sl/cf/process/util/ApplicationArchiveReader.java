package com.sap.cloud.lm.sl.cf.process.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.cloudfoundry.client.lib.domain.CloudResource;

import com.sap.cloud.lm.sl.cf.core.util.FileUtils;
import com.sap.cloud.lm.sl.cf.persistence.services.FileUploader;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.SLException;

public class ApplicationArchiveReader {
    private static final String APPLICATION_ENTRY_DIGEST_METHOD = "SHA";
    private static final int BUFFER_SIZE = 4 * 1024; // 4KB
    private ZipInputStream zipInputStream;
    private String moduleFileName;
    private long maxSizeInBytes;
    private long currentSizeInBytes;
    private DigestCalculator applicationDigestCalculator;

    public ApplicationArchiveReader(InputStream inputStream, String moduleFileName, long maxSizeInBytes) {
        this.zipInputStream = new ZipInputStream(inputStream);
        this.moduleFileName = moduleFileName;
        this.maxSizeInBytes = maxSizeInBytes;
        this.applicationDigestCalculator = createDigestCalculator(FileUploader.DIGEST_METHOD);
    }

    public void initializeApplicationResources(ApplicationResources applicationResources) {
        try {
            setApplicationResources(applicationResources);
        } catch (IOException e) {
            throw new SLException(e, Messages.ERROR_RETRIEVING_MTA_MODULE_CONTENT, moduleFileName);
        }
    }

    private void setApplicationResources(ApplicationResources applicationResources) throws IOException {
        ZipEntry zipEntry = getFirstZipEntry();
        do {
            if (isFile(zipEntry.getName())) {
                addDataToApplicationResources(zipEntry.getName(), applicationResources);
            }
        } while ((zipEntry = getNextEntryByName(moduleFileName)) != null);
        applicationResources.setApplicationDigest(applicationDigestCalculator.getDigest());
    }

    public ZipEntry getFirstZipEntry() throws IOException {
        ZipEntry zipEntry = getNextEntryByName(moduleFileName);
        if (zipEntry == null) {
            throw new ContentException(com.sap.cloud.lm.sl.mta.message.Messages.CANNOT_FIND_ARCHIVE_ENTRY, moduleFileName);
        }
        return zipEntry;
    }

    private void addDataToApplicationResources(String zipEntryName, ApplicationResources applicationResources) throws IOException {
        DigestCalculator entryDigestCalculator = createDigestCalculator(APPLICATION_ENTRY_DIGEST_METHOD);
        byte[] buffer = new byte[BUFFER_SIZE];
        int numberOfReadBytes = 0;
        long sizeOfEntry = 0;
        while ((numberOfReadBytes = zipInputStream.read(buffer)) != -1) {
            if (currentSizeInBytes + numberOfReadBytes > maxSizeInBytes) {
                throw new ContentException(Messages.SIZE_OF_APP_EXCEEDS_MAX_SIZE_LIMIT, maxSizeInBytes);
            }
            sizeOfEntry += numberOfReadBytes;
            entryDigestCalculator.updateDigest(buffer, 0, numberOfReadBytes);
            applicationDigestCalculator.updateDigest(buffer, 0, numberOfReadBytes);
        }
        applicationResources.addCloudResource(new CloudResource(zipEntryName, sizeOfEntry, entryDigestCalculator.getDigest()));
    }

    private DigestCalculator createDigestCalculator(String algorithm) {
        try {
            return new DigestCalculator(MessageDigest.getInstance(algorithm));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public InputStream getZipEntryStream() {
        return zipInputStream;
    }

    public ZipEntry getNextEntryByName(String name) throws IOException {
        for (ZipEntry zipEntry; (zipEntry = zipInputStream.getNextEntry()) != null;) {
            if (zipEntry.getName()
                .startsWith(name)) {
                validateEntry(zipEntry);
                return zipEntry;
            }
        }
        return null;
    }

    protected void validateEntry(ZipEntry entry) {
        FileUtils.validatePath(entry.getName());
    }

    private boolean isFile(String fileName) {
        return !FileUtils.isDirectory(fileName);
    }

}