package com.sap.cloud.lm.sl.cf.process.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.SLException;

public class ApplicationArchiveExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationArchiveExtractor.class);

    static final String PATH_SHOULD_NOT_BE_ABSOLUTE = "Archive entry name \"{0}\" should not be absolute";
    static final String PATH_SHOULD_BE_NORMALIZED = "Archive entry name \"{0}\" should be normalized";
    private static final int BUFFER_SIZE = 4 * 1024; // 4KB
    private static final String ARCHIVE_FILE_SEPARATOR = "/";
    private InputStream appArchiveStream;
    private String fileName;

    public ApplicationArchiveExtractor(InputStream appArchiveStream, String fileName) {
        this.appArchiveStream = appArchiveStream;
        this.fileName = fileName;
    }

    public Path extract(ExtractStatusCallback callback) throws IOException, SLException {
        if (callback == null) {
            callback = ExtractStatusCallback.NONE;
        }

        try (InputStreamProducer streamProducer = new InputStreamProducer(appArchiveStream, fileName)) {
            return extract(streamProducer, callback);
        } catch (IOException | SLException e) {
            callback.onError(e);
            throw e;
        } catch (Exception e) {
            callback.onError(e);
            return null;
        }
    }

    private Path extract(InputStreamProducer streamProducer, ExtractStatusCallback callback) throws IOException, SLException {
        InputStream stream = streamProducer.getNextInputStream();
        if (stream == null) {
            throw new ContentException(com.sap.cloud.lm.sl.mta.message.Messages.CANNOT_FIND_ARCHIVE_ENTRY, streamProducer.getFileName());
        }

        if (isFile(streamProducer.getFileName())) {
            return saveToFile(streamProducer, callback);
        }

        if (streamProducer.getFileName()
            .equals(streamProducer.getStreamEntryName())) {
            return saveToDirectory(streamProducer, callback);
        }

        return saveToDirectoryUnordered(streamProducer, callback);
    }

    private Path saveToFile(InputStreamProducer streamProducer, ExtractStatusCallback callback) throws IOException, SLException {
        String prefix = getPrefix(streamProducer.getStreamEntryName());
        String suffix = getSuffix(streamProducer.getStreamEntryName());

        Path filePath = File.createTempFile(prefix, suffix)
            .toPath();
        callback.onFileCreated(filePath);
        LOGGER.debug(MessageFormat.format("Saving input stream to temporary file \"{0}\"...", filePath));
        try (OutputStream outputStream = Files.newOutputStream(filePath)) {
            copy(streamProducer.getInputStream(), outputStream, callback);
        }
        LOGGER.debug(MessageFormat.format("Input stream saved to temporary file \"{0}\"", filePath));
        return filePath;
    }

    private Path saveToDirectory(InputStreamProducer streamProducer, ExtractStatusCallback callback) throws IOException, SLException {
        Path dirPath = getTempDirectory(streamProducer.getFileName());
        callback.onFileCreated(dirPath);
        LOGGER.debug(MessageFormat.format("Saving input stream to temporary directory \"{0}\"...", dirPath));
        saveEntries(streamProducer, dirPath, callback);
        LOGGER.debug(MessageFormat.format("Input stream saved to temporary directory \"{0}\"", dirPath));
        return dirPath;
    }

    protected Path saveToDirectoryUnordered(InputStreamProducer streamProducer, ExtractStatusCallback callback)
        throws IOException, SLException {
        Path dirPath = getTempDirectory(streamProducer.getFileName());
        callback.onFileCreated(dirPath);
        do {
            if (isFile(streamProducer.getStreamEntryName())) {
                saveStreamToDirectory(streamProducer.getInputStream(), streamProducer.getStreamEntryName(), streamProducer.getFileName(),
                    dirPath, callback);
            }
        } while (streamProducer.getNextInputStream() != null);
        return dirPath;
    }

    private void saveEntries(InputStreamProducer streamProducer, Path dirPath, ExtractStatusCallback callback)
        throws IOException, SLException {
        ZipInputStream inputStream = (ZipInputStream) streamProducer.getInputStream();
        for (ZipEntry entry; (entry = inputStream.getNextEntry()) != null;) {
            if (!entry.getName()
                .startsWith(streamProducer.getStreamEntryName())) {
                continue;
            }
            validateEntry(entry.getName());
            Path filePath = resolveTempEntryPath(entry.getName(), streamProducer.getStreamEntryName(), dirPath);
            saveEntry(streamProducer.getInputStream(), entry, filePath, callback);
        }
    }

    private void copy(InputStream input, OutputStream output, ExtractStatusCallback callback) throws IOException, SLException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int numberOfReadBytes = 0;
        while ((numberOfReadBytes = input.read(buffer)) != -1) {
            callback.onBytesToWrite(numberOfReadBytes);
            output.write(buffer, 0, numberOfReadBytes);
        }
    }

    private void saveEntry(InputStream inputStream, ZipEntry entry, Path filePath, ExtractStatusCallback callback)
        throws IOException, SLException {
        if (!isFile(entry.getName())) {
            Files.createDirectories(filePath);
            return;
        }

        Files.createDirectories(filePath.getParent());
        filePath = Files.createFile(filePath);
        try (OutputStream outputStream = Files.newOutputStream(filePath)) {
            copy(inputStream, outputStream, callback);
        }
    }

    private void saveStreamToDirectory(InputStream inputStream, String entryName, String dirName, Path dirPath,
        ExtractStatusCallback callback) throws IOException, SLException {
        validateEntry(entryName);
        Path filePath = resolveTempEntryPath(entryName, dirName, dirPath);
        Files.createDirectories(filePath.getParent());
        Files.createFile(filePath);
        try (OutputStream outputStream = Files.newOutputStream(filePath)) {
            copy(inputStream, outputStream, callback);
        }
    }

    private boolean isFile(String fileName) {
        return !fileName.endsWith(ARCHIVE_FILE_SEPARATOR);
    }

    private String getPrefix(String nameOfFile) {
        int indexOfDot = nameOfFile.lastIndexOf('.');
        String prefix = (indexOfDot > 0) ? nameOfFile.substring(0, indexOfDot) : nameOfFile;
        if (prefix.length() < 3) {
            prefix = prefix + "000";
        }
        return prefix;
    }

    private String getSuffix(String nameOfFile) {
        int indexOfDot = nameOfFile.lastIndexOf('.');
        return (indexOfDot > 0) ? nameOfFile.substring(indexOfDot) : null;
    }

    public static Path getTempDirectory(String entryName) throws IOException {
        String dirName = entryName;
        if (dirName.endsWith(ARCHIVE_FILE_SEPARATOR)) {
            dirName = dirName.substring(0, dirName.length() - 1);
        }
        dirName = dirName.replace('/', '-');
        Path dirPath = Paths.get(dirName);
        if (Files.exists(dirPath)) {
            return dirPath;
        }
        return Files.createTempDirectory(dirName);
    }

    public static Path resolveTempEntryPath(String entryName, String rootEntryName, Path dirPath) {
        return dirPath.resolve(Paths.get(entryName.substring(rootEntryName.length())));
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
}
