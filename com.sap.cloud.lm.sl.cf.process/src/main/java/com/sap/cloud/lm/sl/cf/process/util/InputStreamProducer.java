package com.sap.cloud.lm.sl.cf.process.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class InputStreamProducer implements Closeable {

    ZipInputStream stream;
    String fileName;
    String streamEntryName = null;

    public InputStreamProducer(InputStream stream, String fileName) {
        this.stream = new ZipInputStream(stream);
        this.fileName = fileName;
    }

    public InputStream getNextInputStream() throws IOException {
        for (ZipEntry zipEntry; (zipEntry = stream.getNextEntry()) != null;) {
            if (zipEntry.getName()
                .startsWith(fileName)) {
                streamEntryName = zipEntry.getName();
                return stream;
            }
        }
        return null;
    }
    
    public InputStream getInputStream() {
        return stream;
    }
    
    public String getFileName() {
        return fileName;
    }

    public String getStreamEntryName() {
        return streamEntryName;
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
