package com.sap.cloud.lm.sl.cf.core.files;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;

public class FilePartsMerger implements Closeable {
    private Path mergedFilePath;
    private OutputStream fileOutputStream;

    public FilePartsMerger(String fileName) throws IOException {
        Path tempDir = Files.createTempDirectory("merge");
        mergedFilePath = Paths.get(tempDir.toString(), fileName);
        fileOutputStream = Files.newOutputStream(mergedFilePath);
    }

    public void merge(InputStream filePartInputStream) throws IOException {
        IOUtils.copy(filePartInputStream, fileOutputStream);
    }

    public Path getMergedFilePath() throws IOException {
        return mergedFilePath;
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(fileOutputStream);
    }

}
