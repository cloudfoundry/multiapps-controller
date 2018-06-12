package com.sap.cloud.lm.sl.cf.client.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.client.message.Messages;
import com.sap.cloud.lm.sl.common.ContentException;

public class StreamUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamUtil.class);

    static final String PATH_SHOULD_NOT_BE_ABSOLUTE = "Archive entry name \"{0}\" should not be absolute";
    static final String PATH_SHOULD_BE_NORMALIZED = "Archive entry name \"{0}\" should be normalized";
    static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
    public static final String ARCHIVE_ENTRY_SEPARATOR = "/";
    private InputStream inputStream;

    public StreamUtil(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public File saveStreamToFile(String entryName, Path tempDirectory, long maxZipEntrySize) throws IOException {
        int i = entryName.lastIndexOf('.');
        String fileName = (i > 0) ? entryName.substring(0, i) : entryName;
        String suffix = (i > 0) ? entryName.substring(i) : null;
        if (fileName.length() < 3) {
            fileName = fileName + "000";
        }
        File file = File.createTempFile(fileName, suffix, tempDirectory.toFile());
        LOGGER.debug(MessageFormat.format("Saving input stream to temporary file \"{0}\"...", file.getPath()));
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            limitCopy(inputStream, outputStream, maxZipEntrySize);
        }
        LOGGER.debug(MessageFormat.format("Input stream saved to temporary file \"{0}\"", file.getPath()));
        return file;
    }

    public long saveStreamToDirectory(String entryName, String directoryName, Path tempDirectory, long startSize, long maxZipEntrySize)
        throws IOException {
        validateEntry(entryName);
        Path filePath = resolveTempEntryPath(entryName, directoryName, tempDirectory);
        createParentDirectories(filePath);
        Files.createFile(filePath);
        try (OutputStream outputStream = Files.newOutputStream(filePath)) {
            return limitCopy(inputStream, outputStream, startSize, maxZipEntrySize);
        }
    }

    public File saveZipStreamToDirectory(String entryName, Path tempDirectory, long maxZipEntrySize) throws IOException {
        File dir = tempDirectory.toFile();
        LOGGER.debug(MessageFormat.format("Saving input stream to temporary directory \"{0}\"...", dir.getPath()));
        saveEntries(tempDirectory, entryName, maxZipEntrySize);
        LOGGER.debug(MessageFormat.format("Input stream saved to temporary directory \"{0}\"", dir.getPath()));
        return dir;
    }

    private void saveEntries(Path tempDirectory, String rootEntryName, long maxZipEntrySize) throws IOException, FileNotFoundException {
        ZipInputStream zis = (ZipInputStream) inputStream;
        long filesSize = 0;
        for (ZipEntry e; (e = zis.getNextEntry()) != null;) {
            String currentEntryName = e.getName();
            if (!e.getName()
                .startsWith(rootEntryName)) {
                continue;
            }
            validateEntry(currentEntryName);
            Path filePath = resolveTempEntryPath(currentEntryName, rootEntryName, tempDirectory);
            if (e.getName()
                .endsWith(ARCHIVE_ENTRY_SEPARATOR)) {
                Files.createDirectories(filePath);
            } else {
                Files.createDirectories(filePath.getParent());
                Files.createFile(filePath);
                try (OutputStream outputStream = Files.newOutputStream(filePath)) {
                    filesSize = limitCopy(inputStream, outputStream, filesSize, maxZipEntrySize);
                }
            }
        }
    }

    private void limitCopy(InputStream input, OutputStream output, long maxSize) throws IOException {
        limitCopy(input, output, new byte[DEFAULT_BUFFER_SIZE], 0, maxSize);
    }

    private long limitCopy(InputStream input, OutputStream output, long startSize, long maxSize) throws IOException {
        return limitCopy(input, output, new byte[DEFAULT_BUFFER_SIZE], startSize, maxSize);
    }

    private long limitCopy(InputStream input, OutputStream output, byte[] buffer, long startSize, long maxSize) throws IOException {
        int numberOfReadBytes = 0;
        long totalSize = startSize;
        while ((numberOfReadBytes = input.read(buffer)) != -1) {
            if (totalSize + numberOfReadBytes > maxSize) {
                throw new ContentException(Messages.ERROR_SIZE_OF_APPLICATION_EXCEEDS_MAX_SIZE_LIMIT, maxSize);
            }
            totalSize += numberOfReadBytes;
            output.write(buffer, 0, numberOfReadBytes);
        }
        return totalSize;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public static boolean isArchiveEntryDirectory(String fileName) {
        return fileName.endsWith(ARCHIVE_ENTRY_SEPARATOR);
    }

    public static Path resolveTempEntryPath(String entryName, String rootEntryName, Path tempDir) {
        return tempDir.resolve(Paths.get(entryName.substring(rootEntryName.length())));
    }

    public static void validateEntry(String entryName) {
        if (!entryName.equals(FilenameUtils.normalize(entryName, true))) {
            throw new IllegalArgumentException(MessageFormat.format(PATH_SHOULD_BE_NORMALIZED, entryName));
        }
        if (Paths.get(entryName)
            .isAbsolute()) {
            throw new IllegalArgumentException(MessageFormat.format(PATH_SHOULD_NOT_BE_ABSOLUTE, entryName));
        }
    }

    public static void createParentDirectories(Path entryPath) throws IOException {
        if (!Files.exists(entryPath.getParent())) {
            Files.createDirectories(entryPath.getParent());
        }
    }

    public static Path getTempDirectoryFromFilename(String fileName) throws IOException {
        String dirName = Paths.get(fileName)
            .getFileName()
            .toString()
            .replace('.', '-');
        return Files.createTempDirectory(dirName);
    }

    public static Path getTempDirectory(String entryName) throws IOException {
        String dirName = entryName;
        if (dirName.endsWith(ARCHIVE_ENTRY_SEPARATOR)) {
            dirName = dirName.substring(0, dirName.length() - 1);
        }
        dirName = dirName.replace('/', '-');
        Path dirPath = Paths.get(dirName);
        if (Files.exists(dirPath)) {
            return dirPath;
        }
        return Files.createTempDirectory(dirName);
    }

    public static void deleteFile(File file) throws IOException {
        if (file != null && file.exists()) {
            if (file.isDirectory()) {
                FileUtils.deleteDirectory(file);
            } else {
                Files.delete(Paths.get(file.getPath()));
            }
        }
    }

    public static byte[] removeLeadingLine(byte[] data) {
        if (data == null || data.length == 0) {
            return data;
        }
        int from = 0;
        if (data[0] == '\n') {
            from = 1;
        }
        if (data[0] == '\r' && data[1] == '\n') {
            from = 2;
        }
        return Arrays.copyOfRange(data, from, data.length);
    }
}
