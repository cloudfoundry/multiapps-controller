package com.sap.cloud.lm.sl.cf.process.util;

import java.time.OffsetDateTime;
import java.util.zip.ZipEntry;

public class UtcAdjustedZipEntry extends ZipEntry {

    public UtcAdjustedZipEntry(String name) {
        super(name);
        setTime(OffsetDateTime.now()
                              .toEpochSecond());
    }

    public UtcAdjustedZipEntry(ZipEntry e) {
        super(e);
        setTime(OffsetDateTime.now()
                              .toEpochSecond());
    }
}