package com.sap.cloud.lm.sl.cf.core.files;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;

public class FileSplitter {
    private String fileName;
    private String filePath;
    private long fileSize;
    private long filePartSize;
    private int filePartIndex = 0;

    private List<FilePart> fileParts = new ArrayList<>();
    private InputStream fileInputStream;
    private FilePartConfiguration configuration;

    public FileSplitter(Path filePath) throws IOException {
        this(filePath.getFileName()
            .toString(),
            filePath.toAbsolutePath()
                .toString(),
            Files.newInputStream(filePath), null);
        this.fileSize = Files.size(filePath);
    }

    public FileSplitter(String fileName, String filePath, InputStream fileInputStream, FilePartConfiguration configuration)
        throws IOException {
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileInputStream = fileInputStream;
        this.configuration = getConfiguration(configuration);
    }

    private FilePartConfiguration getConfiguration(FilePartConfiguration configuration) {
        if (configuration == null) {
            return FilePartConfiguration.DEFAULT_FILE_PART_CONFIGURATION;
        }
        return configuration;
    }

    public List<FilePart> splitFile() throws IOException {
        if (fileSize <= configuration.getMaxFileSize()) {
            return Arrays.asList(new FilePart(fileName, filePath));
        }
        try (OutputStream filePartOutputStream = createFilePartOutputStream()) {
            writeFilePart(filePartOutputStream);
        } finally {
            IOUtils.closeQuietly(fileInputStream);
        }

        return fileParts;
    }

    private void writeFilePart(OutputStream filePartOutputStream) throws IOException {
        int len;
        byte[] buf = new byte[1024];
        while ((len = fileInputStream.read(buf)) >= 0) {
            if (len + filePartSize > configuration.getMaxFileSize()) {
                filePartOutputStream.close();
                filePartOutputStream = createFilePartOutputStream();
            }
            filePartSize += len;
            filePartOutputStream.write(buf, 0, len);
        }
    }

    private OutputStream createFilePartOutputStream() throws IOException {
        String filePartName = constructFilePartName();
        fileParts.add(new FilePart(filePartName, getFilePartPath(filePartName)));
        filePartIndex++;
        filePartSize = 0;
        return Files.newOutputStream(Paths.get(getFilePartPath(filePartName)));
    }

    private String getFilePartPath(String filePartName) throws IOException {
        return configuration.getFilePartsPath() + File.separator + filePartName;
    }

    private String constructFilePartName() {
        StringBuilder partNameBuilder = new StringBuilder(fileName);
        partNameBuilder.append(configuration.getPartPostfix());
        partNameBuilder.append(filePartIndex);
        return partNameBuilder.toString();
    }

}
