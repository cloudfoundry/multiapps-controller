package com.sap.cloud.lm.sl.cf.client.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class InputStreamProducer implements Closeable {

    ZipInputStream stream;
    String entryName;
    String streamEntryName = null;
    private long maxEntrySize;

    public InputStreamProducer(InputStream stream, String entryName, long maxEntrySize) {
        this.stream = new ZipInputStream(stream);
        this.entryName = entryName;
        this.maxEntrySize = maxEntrySize;
    }

    public InputStream getNextInputStream() throws IOException {
        for (ZipEntry zipEntry; (zipEntry = stream.getNextEntry()) != null;) {
            if (zipEntry.getName()
                .startsWith(entryName)) {
                streamEntryName = zipEntry.getName();
                return stream;
            }
        }
        return null;
    }

    public String getStreamEntryName() {
        return streamEntryName;
    }

    public long getMaxEntrySize() {
        return maxEntrySize;
    }

    public void setMaxEntrySize(long maxEntrySize) {
        this.maxEntrySize = maxEntrySize;
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
