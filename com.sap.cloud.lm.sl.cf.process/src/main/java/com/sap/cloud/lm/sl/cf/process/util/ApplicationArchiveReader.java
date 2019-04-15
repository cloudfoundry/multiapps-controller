package com.sap.cloud.lm.sl.cf.process.util;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.cloudfoundry.client.lib.domain.CloudResource;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.util.FileUtils;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.SLException;

@Component
@Profile("cf")
public class ApplicationArchiveReader {
    private static final String APPLICATION_ENTRY_DIGEST_METHOD = "SHA";
    protected static final int BUFFER_SIZE = 4 * 1024; // 4KB

    public void initializeApplicationResources(ApplicationArchiveContext applicationArchiveContext,
        ApplicationResources applicationResources) {
        try {
            setApplicationResources(applicationArchiveContext, applicationResources);
        } catch (IOException e) {
            throw new SLException(e, Messages.ERROR_RETRIEVING_MTA_MODULE_CONTENT, applicationArchiveContext.getModuleFileName());
        }
    }

    private void setApplicationResources(ApplicationArchiveContext applicationArchiveContext, ApplicationResources applicationResources)
        throws IOException {
        String moduleFileName = applicationArchiveContext.getModuleFileName();
        ZipEntry zipEntry = getFirstZipEntry(applicationArchiveContext);
        do {
            if (isFile(zipEntry.getName())) {
                addDataToApplicationResources(zipEntry.getName(), applicationResources, applicationArchiveContext);
            }
        } while ((zipEntry = getNextEntryByName(moduleFileName, applicationArchiveContext)) != null);
        applicationResources.setApplicationDigest(applicationArchiveContext.getApplicationDigestCalculator()
            .getDigest());
    }

    public ZipEntry getFirstZipEntry(ApplicationArchiveContext applicationArchiveContext) throws IOException {
        String moduleFileName = applicationArchiveContext.getModuleFileName();
        ZipEntry zipEntry = getNextEntryByName(moduleFileName, applicationArchiveContext);
        if (zipEntry == null) {
            throw new ContentException(com.sap.cloud.lm.sl.mta.message.Messages.CANNOT_FIND_ARCHIVE_ENTRY, moduleFileName);
        }
        return zipEntry;
    }

    protected void addDataToApplicationResources(String zipEntryName, ApplicationResources applicationResources,
        ApplicationArchiveContext applicationArchiveContext) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int numberOfReadBytes = 0;
        long sizeOfEntry = 0;
        ZipInputStream zipInputStream = applicationArchiveContext.getZipInputStream();
        long maxSizeInBytes = applicationArchiveContext.getMaxSizeInBytes();
        DigestCalculator applicationDigestCalculator = applicationArchiveContext.getApplicationDigestCalculator();
        DigestCalculator entryDigestCalculator = createDigestCalculator(APPLICATION_ENTRY_DIGEST_METHOD);

        while ((numberOfReadBytes = zipInputStream.read(buffer)) != -1) {
            long currentSizeInBytes = applicationArchiveContext.getCurrentSizeInBytes();
            if (currentSizeInBytes + numberOfReadBytes > maxSizeInBytes) {
                throw new ContentException(Messages.SIZE_OF_APP_EXCEEDS_MAX_SIZE_LIMIT, maxSizeInBytes);
            }
            sizeOfEntry += numberOfReadBytes;
            applicationArchiveContext.calculateCurrentSizeInBytes(numberOfReadBytes);
            entryDigestCalculator.updateDigest(buffer, 0, numberOfReadBytes);
            applicationDigestCalculator.updateDigest(buffer, 0, numberOfReadBytes);
        }
        String relativeZipEntryName = getZipEntryNameWithoutParent(applicationArchiveContext.getModuleFileName(), zipEntryName);
        applicationResources.addCloudResource(new CloudResource(relativeZipEntryName, sizeOfEntry, entryDigestCalculator.getDigest()));
    }

    private String getZipEntryNameWithoutParent(String moduleFileName, String zipEntryName) {
        Path moduleFileNamePathParent = Paths.get(moduleFileName)
            .getParent();
        if (moduleFileNamePathParent == null) {
            return FileUtils.getRelativePath(moduleFileName, zipEntryName);
        }
        return FileUtils.getRelativePath(moduleFileNamePathParent.toString(), zipEntryName);
    }

    private DigestCalculator createDigestCalculator(String algorithm) {
        try {
            return new DigestCalculator(MessageDigest.getInstance(algorithm));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public ZipEntry getNextEntryByName(String name, ApplicationArchiveContext applicationArchiveContext) throws IOException {
        ZipInputStream zipInputStream = applicationArchiveContext.getZipInputStream();
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