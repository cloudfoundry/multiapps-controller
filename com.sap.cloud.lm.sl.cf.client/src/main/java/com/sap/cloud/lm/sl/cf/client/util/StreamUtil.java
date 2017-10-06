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
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamUtil.class);

    static final String PATH_SHOULD_NOT_BE_ABSOLUTE = "Archive entry name \"{0}\" should not be absolute";
    static final String PATH_SHOULD_BE_NORMALIZED = "Archive entry name \"{0}\" should be normalized";
    static final String ARCHIVE_ENTRY_SEPARATOR = "/";

    public static File saveStream(String fileName, InputStream inputStream) throws IOException {
        if (fileName.endsWith(ARCHIVE_ENTRY_SEPARATOR)) {
            return saveZipStreamToDirectory(fileName, inputStream);
        } else {
            return saveStreamToFile(fileName, inputStream);
        }
    }

    public static File saveStreamToFile(String entryName, InputStream inputStream) throws IOException {
        int i = entryName.lastIndexOf('.');
        String fileName = (i > 0) ? entryName.substring(0, i) : entryName;
        String suffix = (i > 0) ? entryName.substring(i) : null;
        if (fileName.length() < 3) {
            fileName = fileName + "000";
        }
        File file = File.createTempFile(fileName, suffix);
        LOGGER.debug(MessageFormat.format("Saving input stream to temporary file \"{0}\"...", file.getPath()));
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            IOUtils.copy(inputStream, outputStream);
        }
        LOGGER.debug(MessageFormat.format("Input stream saved to temporary file \"{0}\"", file.getPath()));
        return file;
    }

    public static void saveStreamToDirectory(String entryName, String directoryName, Path tempDirectory, InputStream inputStream)
        throws IOException {
        validateEntry(entryName);
        Path filePath = resolveTempEntryPath(entryName, directoryName, tempDirectory);
        createParentDirectories(filePath);
        createFile(filePath, inputStream);
    }

    public static File saveZipStreamToDirectory(String entryName, InputStream inputStream) throws IOException {
        Path path = getTempDirectory(entryName);
        File dir = path.toFile();
        LOGGER.debug(MessageFormat.format("Saving input stream to temporary directory \"{0}\"...", dir.getPath()));
        saveEntries(path, entryName, inputStream);
        LOGGER.debug(MessageFormat.format("Input stream saved to temporary directory \"{0}\"", dir.getPath()));
        return dir;
    }

    private static void createParentDirectories(Path entryPath) throws IOException {
        if (!Files.exists(entryPath.getParent())) {
            Files.createDirectories(entryPath.getParent());
        }
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

    private static void saveEntries(Path tempDirectory, String rootEntryName, InputStream inputStream)
        throws IOException, FileNotFoundException {
        ZipInputStream zis = (ZipInputStream) inputStream;
        for (ZipEntry e; (e = zis.getNextEntry()) != null;) {
            String currentEntryName = e.getName();
            if (!e.getName().startsWith(rootEntryName)) {
                continue;
            }
            validateEntry(currentEntryName);
            Path filePath = resolveTempEntryPath(currentEntryName, rootEntryName, tempDirectory);
            if (e.getName().endsWith(ARCHIVE_ENTRY_SEPARATOR)) {
                Files.createDirectories(filePath);
            } else {
                createFile(filePath, inputStream);
            }
        }
    }

    private static void createFile(Path fileToCreate, InputStream inputStream) throws IOException {
        Files.createFile(fileToCreate);
        try (OutputStream outputStream = Files.newOutputStream(fileToCreate)) {
            IOUtils.copy(inputStream, outputStream);
        }
    }

    private static Path resolveTempEntryPath(String entryName, String rootEntryName, Path tempDir) {
        return tempDir.resolve(Paths.get(entryName.substring(rootEntryName.length())));
    }

    private static void validateEntry(String entryName) {
        if (!entryName.equals(FilenameUtils.normalize(entryName, true))) {
            throw new IllegalArgumentException(MessageFormat.format(PATH_SHOULD_BE_NORMALIZED, entryName));
        }
        if (Paths.get(entryName).isAbsolute()) {
            throw new IllegalArgumentException(MessageFormat.format(PATH_SHOULD_NOT_BE_ABSOLUTE, entryName));
        }
    }
}
