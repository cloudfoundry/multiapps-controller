package org.cloudfoundry.multiapps.controller.process.util;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.inject.Named;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.util.FileUtils;
import org.cloudfoundry.multiapps.controller.process.Messages;

@Named
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
            if (!zipEntry.isDirectory()) {
                calculateDigestFromArchive(applicationArchiveContext);
            }
        } while ((zipEntry = getNextEntryByName(moduleFileName, applicationArchiveContext)) != null);
    }

    public ZipEntry getFirstZipEntry(ApplicationArchiveContext applicationArchiveContext) throws IOException {
        String moduleFileName = applicationArchiveContext.getModuleFileName();
        ZipEntry zipEntry = getNextEntryByName(moduleFileName, applicationArchiveContext);
        if (zipEntry == null) {
            throw new ContentException(org.cloudfoundry.multiapps.mta.Messages.CANNOT_FIND_ARCHIVE_ENTRY, moduleFileName);
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

}