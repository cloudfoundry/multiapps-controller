package com.sap.cloud.lm.sl.cf.process.util;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.util.FileUtils;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.SLException;

@Component
public class ApplicationArchiveReader {
    protected static final int BUFFER_SIZE = 4 * 1024; // 4KB

    public String calculateApplicationDigest(ApplicationArchiveContext applicationArchiveContext) {
        try {
            iterateApplicationArchive(applicationArchiveContext);
            return applicationArchiveContext.getApplicationDigestCalculator()
                                            .getDigest();
        } catch (IOException e) {
            throw new SLException(e, Messages.ERROR_RETRIEVING_MTA_MODULE_CONTENT, applicationArchiveContext.getModuleFileName());
        }
    }

    private void iterateApplicationArchive(ApplicationArchiveContext applicationArchiveContext) throws IOException {
        String moduleFileName = applicationArchiveContext.getModuleFileName();
        ZipEntry zipEntry = getFirstZipEntry(applicationArchiveContext);
        do {
            if (isFile(zipEntry.getName())) {
                calculateDigestFromArchive(applicationArchiveContext);
            }
        } while ((zipEntry = getNextEntryByName(moduleFileName, applicationArchiveContext)) != null);
    }

    public ZipEntry getFirstZipEntry(ApplicationArchiveContext applicationArchiveContext) throws IOException {
        String moduleFileName = applicationArchiveContext.getModuleFileName();
        ZipEntry zipEntry = getNextEntryByName(moduleFileName, applicationArchiveContext);
        if (zipEntry == null) {
            throw new ContentException(com.sap.cloud.lm.sl.mta.message.Messages.CANNOT_FIND_ARCHIVE_ENTRY, moduleFileName);
        }
        return zipEntry;
    }

    protected void calculateDigestFromArchive(ApplicationArchiveContext applicationArchiveContext) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int numberOfReadBytes = 0;
        ZipInputStream zipInputStream = applicationArchiveContext.getZipInputStream();
        long maxSizeInBytes = applicationArchiveContext.getMaxSizeInBytes();
        DigestCalculator applicationDigestCalculator = applicationArchiveContext.getApplicationDigestCalculator();

        while ((numberOfReadBytes = zipInputStream.read(buffer)) != -1) {
            long currentSizeInBytes = applicationArchiveContext.getCurrentSizeInBytes();
            if (currentSizeInBytes + numberOfReadBytes > maxSizeInBytes) {
                throw new ContentException(Messages.SIZE_OF_APP_EXCEEDS_MAX_SIZE_LIMIT, maxSizeInBytes);
            }
            applicationArchiveContext.calculateCurrentSizeInBytes(numberOfReadBytes);
            applicationDigestCalculator.updateDigest(buffer, 0, numberOfReadBytes);
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