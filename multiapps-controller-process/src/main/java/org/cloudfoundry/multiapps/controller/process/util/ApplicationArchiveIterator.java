package org.cloudfoundry.multiapps.controller.process.util;

import java.io.IOException;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.core.util.FileUtils;

import jakarta.inject.Named;

@Named
public class ApplicationArchiveIterator {

    public ZipArchiveEntry getFirstZipEntry(String moduleFileName, ZipArchiveInputStream zipArchiveInputStream) throws IOException {
        ZipArchiveEntry zipEntry = getNextEntryByName(moduleFileName, zipArchiveInputStream);
        if (zipEntry == null) {
            throw new ContentException(org.cloudfoundry.multiapps.mta.Messages.CANNOT_FIND_ARCHIVE_ENTRY, moduleFileName);
        }
        return zipEntry;
    }

    public ZipArchiveEntry getNextEntryByName(String name, ZipArchiveInputStream zipArchiveInputStream) throws IOException {
        for (ZipArchiveEntry zipEntry; (zipEntry = zipArchiveInputStream.getNextEntry()) != null;) {
            if (zipEntry.getName()
                        .startsWith(name)) {
                validateEntry(zipEntry);
                return zipEntry;
            }
        }
        return null;
    }

    protected void validateEntry(ZipArchiveEntry entry) {
        FileUtils.validatePath(entry.getName());
    }

}
