package org.cloudfoundry.multiapps.controller.process.util;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jakarta.inject.Named;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.core.util.FileUtils;

@Named
public class ApplicationArchiveReader {

    public ZipEntry getFirstZipEntry(ApplicationArchiveContext applicationArchiveContext, ZipInputStream zipArchiveInputStream)
        throws IOException {
        String moduleFileName = applicationArchiveContext.getModuleFileName();
        ZipEntry zipEntry = getNextEntryByName(moduleFileName, zipArchiveInputStream);
        if (zipEntry == null) {
            throw new ContentException(org.cloudfoundry.multiapps.mta.Messages.CANNOT_FIND_ARCHIVE_ENTRY, moduleFileName);
        }
        return zipEntry;
    }

    public ZipEntry getNextEntryByName(String name, ZipInputStream zipArchiveInputStream) throws IOException {
        for (ZipEntry zipEntry; (zipEntry = zipArchiveInputStream.getNextEntry()) != null;) {
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
