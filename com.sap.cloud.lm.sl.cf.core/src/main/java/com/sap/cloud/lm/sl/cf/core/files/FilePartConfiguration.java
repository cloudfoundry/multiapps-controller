package com.sap.cloud.lm.sl.cf.core.files;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FilePartConfiguration {
    private static final String FILE_PARTS_TEMP_DIR_PREFIX = "filePartsTempDir";
    private static final long DEFAULT_MAX_FILE_SIZE = 50000000; // DEFAULT MAX SIZE PART - 50 MB
    private static final String DEFAULT_PART_POSTFIX = ".part.";
    private static final String DEFAULT_PART_EXTENSION = ".mtar";

    public static final FilePartConfiguration DEFAULT_FILE_PART_CONFIGURATION = new FilePartConfiguration(DEFAULT_PART_EXTENSION,
                                                                                                          DEFAULT_MAX_FILE_SIZE,
                                                                                                          DEFAULT_PART_POSTFIX);

    private String fileExtension;
    private long maxFileSize;
    private String partPostfix;
    private Path filePartsPath;

    public FilePartConfiguration() {
        this(DEFAULT_PART_EXTENSION, DEFAULT_MAX_FILE_SIZE, DEFAULT_PART_POSTFIX);
    }

    public FilePartConfiguration(String fileExtension, long maxFileSize, String partPostfix) {
        this.fileExtension = fileExtension;
        this.maxFileSize = maxFileSize;
        this.partPostfix = partPostfix;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public String getPartPostfix() {
        return partPostfix;
    }

    public String getFilePartsPath() throws IOException {
        if (filePartsPath == null) {
            filePartsPath = Files.createTempDirectory(FILE_PARTS_TEMP_DIR_PREFIX);
        }
        return filePartsPath.toAbsolutePath()
                            .toString();
    }

}
