package com.sap.cloud.lm.sl.cf.client.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.client.message.Messages;

public class StreamManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamManager.class);
    private InputStream inputStream;
    public static final String ARCHIVE_ENTRY_SEPARATOR = "/";

    public StreamManager(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public File saveStreamToFile(String entryName) throws IOException {
        
        File file = createTempFile(entryName);
        LOGGER.debug(MessageFormat.format(Messages.SAVING_INPUT_STREAM_TEMP_FILE, file.getPath()));
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            IOUtils.copy(inputStream, outputStream);
        }
        LOGGER.debug(MessageFormat.format(Messages.INPUT_STREAM_SAVED_IN_TEMP_FILE, file.getPath()));
        return file;
    }

    private File createTempFile(String entryName) throws IOException {

        int dotIndex = entryName.lastIndexOf('.');
        String fileName = (dotIndex > 0) ? entryName.substring(0, dotIndex) : entryName;
        String suffix = (dotIndex > 0) ? entryName.substring(dotIndex) : null;
        if (fileName.length() < 3) {
            fileName = fileName + "000";
        }
        return File.createTempFile(fileName, suffix);
    }

    public void saveStreamToDirectory(String entryName, String directoryName, Path tempDirectory) throws IOException {
        StreamUtil.validateEntry(entryName);
        Path filePath = StreamUtil.resolveTempEntryPath(entryName, directoryName, tempDirectory);
        StreamUtil.createParentDirectories(filePath);
        createFile(filePath);
    }

    public File saveZipStreamToDirectory(String entryName, long maxZipEntrySize) throws IOException {
        Path path = StreamUtil.getTempDirectory(entryName);
        File dir = path.toFile();
        LOGGER.debug(MessageFormat.format(Messages.SAVING_INPUT_STREAM_TO_TMP_DIR, dir.getPath()));
        saveEntriesToTempDirectory(path, entryName, maxZipEntrySize);
        LOGGER.debug(MessageFormat.format(Messages.INPUT_STREAM_SAVED_TO_TMP_DIR, dir.getPath()));
        return dir;
    }

    private void saveEntriesToTempDirectory(Path tempDirectory, String rootEntryName, long maxZipEntrySize)
        throws IOException, FileNotFoundException {
        ZipInputStream zipInputStream = (ZipInputStream) inputStream;
        for (ZipEntry zipEntry; (zipEntry = zipInputStream.getNextEntry()) != null;) {
            String currentEntryName = zipEntry.getName();
            if (!zipEntry.getName().startsWith(rootEntryName)) {
                continue;
            }
            StreamUtil.validateZipEntrySize(zipEntry, maxZipEntrySize);
            StreamUtil.validateEntry(currentEntryName);
            Path filePath = StreamUtil.resolveTempEntryPath(currentEntryName, rootEntryName, tempDirectory);
            if (zipEntry.getName().endsWith(ARCHIVE_ENTRY_SEPARATOR)) {
                Files.createDirectories(filePath);
            } else {
                createFile(filePath);
            }
        }
    }

    private void createFile(Path fileToCreate) throws IOException {
        Files.copy(inputStream, fileToCreate);
    }
}
