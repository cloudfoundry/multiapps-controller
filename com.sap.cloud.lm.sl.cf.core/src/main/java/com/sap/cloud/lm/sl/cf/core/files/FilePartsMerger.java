package com.sap.cloud.lm.sl.cf.core.files;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.sap.cloud.lm.sl.common.SLException;

public class FilePartsMerger implements Closeable {

    private Path mergedFilePath;
    private OutputStream fileOutputStream;

    public FilePartsMerger(String fileName) {
        try {
            Path tempDir = Files.createTempDirectory("merge");
            mergedFilePath = Paths.get(tempDir.toString(), fileName);
            fileOutputStream = Files.newOutputStream(mergedFilePath);
        } catch (IOException e) {
            cleanUp();
            throw new SLException(e, e.getMessage());
        }
    }

    public void merge(InputStream filePartInputStream) throws IOException {
        IOUtils.copy(filePartInputStream, fileOutputStream);
    }

    public Path getMergedFilePath() {
        return mergedFilePath;
    }

    @Override
    public void close() {
        try {
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        } catch (IOException ioe) {
            // ignore
        }
    }

    public void cleanUp() {
        FileUtils.deleteQuietly(toFile(mergedFilePath));
    }

    private File toFile(Path path) {
        return path != null ? path.toFile() : null;
    }
}
