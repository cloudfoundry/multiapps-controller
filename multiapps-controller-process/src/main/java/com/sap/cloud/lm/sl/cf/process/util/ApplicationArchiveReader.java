package com.sap.cloud.lm.sl.cf.process.util;

import static com.sap.cloud.lm.sl.mta.message.Messages.CANNOT_FIND_ARCHIVE_ENTRY;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.inject.Named;

import com.sap.cloud.lm.sl.cf.core.util.FileUtils;
import com.sap.cloud.lm.sl.common.ContentException;

@Named
public class ApplicationArchiveReader {

    public ZipEntry getFirstZipEntry(ApplicationArchiveContext applicationArchiveContext) throws IOException {
        String moduleFileName = applicationArchiveContext.getModuleFileName();
        ZipEntry zipEntry = getNextEntryByName(moduleFileName, applicationArchiveContext);
        if (zipEntry == null) {
            throw new ContentException(CANNOT_FIND_ARCHIVE_ENTRY, moduleFileName);
        }
        return zipEntry;
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