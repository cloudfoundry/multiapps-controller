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

    public InputStreamProducer(InputStream stream, String entryName) {
        this.stream = new ZipInputStream(stream);
        this.entryName = entryName;
    }

    public InputStream getNextInputStream() throws IOException {
        for (ZipEntry e; (e = stream.getNextEntry()) != null;) {
            if (e.getName().startsWith(entryName)) {
                streamEntryName = e.getName();
                return stream;
            }
        }
        return null;
    }

    public String getStreamEntryName() {
        return streamEntryName;
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
