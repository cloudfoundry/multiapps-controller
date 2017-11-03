package com.sap.cloud.lm.sl.cf.process;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.sap.cloud.lm.sl.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.persistence.processors.DefaultFileDownloadProcessor;
import com.sap.cloud.lm.sl.persistence.processors.DefaultFileUploadProcessor;
import com.sap.cloud.lm.sl.persistence.services.DatabaseFileService;
import com.sap.cloud.lm.sl.persistence.services.FileContentProcessor;
import com.sap.cloud.lm.sl.persistence.services.FileStorageException;

public class ProcessLogsPersistenceService extends DatabaseFileService {

    private static final String LOG_FILE_EXTENSION = ".log";
    private static final String TABLE_NAME = "process_log";
    private static final String DEFAULT_SPACE = "DEFAULT";

    private static ProcessLogsPersistenceService instance;

    public static ProcessLogsPersistenceService getInstance() {
        if (instance == null) {
            instance = new ProcessLogsPersistenceService();
        }
        return instance;
    }

    public ProcessLogsPersistenceService() {
        super(TABLE_NAME);
    }

    public List<String> getLogNames(String namespace) throws FileStorageException {
        return getLogNames(DEFAULT_SPACE, namespace);
    }

    public List<String> getLogNames(String space, String namespace) throws FileStorageException {
        List<String> result = new ArrayList<String>();
        List<FileEntry> logFiles = listFiles(space, namespace);
        for (FileEntry logFile : logFiles) {
            result.add(logFile.getName());
        }
        return result;
    }

    public String getLogContent(String namespace, String logName) throws FileStorageException {
        return getLogContent(DEFAULT_SPACE, namespace, logName);
    }

    public String getLogContent(String space, String namespace, String logName) throws FileStorageException {
        final StringBuilder builder = new StringBuilder();
        String logId = findFileId(space, namespace, logName);

        FileContentProcessor streamProcessor = new FileContentProcessor() {
            @Override
            public void processFileContent(InputStream is) throws IOException {
                builder.append(IOUtils.toString(is));
            }
        };
        processFileContent(new DefaultFileDownloadProcessor(space, logId, streamProcessor));
        return builder.toString();
    }

    public static File getFile(String logId, String name, String logDir) {
        return new java.io.File(logDir, getFileName(logId, name));
    }

    public void saveLog(String namespace, String logName, String logDir) throws IOException, FileStorageException {
        saveLog(DEFAULT_SPACE, namespace, logName, logDir);
    }

    public void saveLog(String space, String namespace, String logName, String logDir) throws IOException, FileStorageException {
        File logFile = getFile(namespace, logName, logDir);
        // we have no concerns of DOS attack, the files are coming from our system

        InputStream in = null;
        try {
            in = new FileInputStream(logFile);
            saveLog(space, namespace, logName, in);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public void saveLog(String space, String namespace, String logName, InputStream in) throws FileStorageException {
        String fileId = findFileId(space, namespace, logName);
        if (fileId != null) {
            deleteFile(space, fileId);
        }

        addFile(space, namespace, logName, new DefaultFileUploadProcessor(false), in);
    }

    public boolean deleteLog(String space, String namespace, String logName) throws FileStorageException {
        String fileId = findFileId(space, namespace, logName);
        return deleteLog(space, fileId);
    }

    public boolean deleteLog(String space, String fileId) throws FileStorageException {
        int rowsDeleted = deleteFile(space, fileId);
        return (rowsDeleted > 0) ? true : false;
    }

    public String findFileId(String namespace, String fileName) throws FileStorageException {
        return findFileId(DEFAULT_SPACE, namespace, fileName);
    }

    public String findFileId(String space, String namespace, String fileName) throws FileStorageException {
        List<FileEntry> listFiles = listFiles(space, namespace);
        for (FileEntry fileEntry : listFiles) {
            if (fileEntry.getName().equals(fileName)) {
                return fileEntry.getId();
            }
        }
        return null;
    }

    private static String getFileName(String logId, String name) {
        return new StringBuilder(logId).append(".").append(name).append(LOG_FILE_EXTENSION).toString();
    }

    public void appendLog(String namespace, String logName, String logDir) throws IOException, FileStorageException {
        appendLog(DEFAULT_SPACE, namespace, logName, logDir);
    }

    public void appendLog(String space, String namespace, String logName, String logDir) throws IOException, FileStorageException {
        File logFile = getFile(namespace, logName, logDir);
        // we have no concerns of DOS attack, the files are coming from our system

        InputStream in = null;
        try {
            in = new FileInputStream(logFile);
            appendLog(space, namespace, logName, in);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    private void appendLog(final String space, final String namespace, final String logName, final InputStream in)
        throws FileStorageException {
        final String fileId = findFileId(space, namespace, logName);
        if (fileId == null) {
            addFile(space, namespace, logName, new DefaultFileUploadProcessor(false), in);
            return;
        }

        FileContentProcessor fileProcessor = new FileContentProcessor() {
            @Override
            public void processFileContent(InputStream logFile) throws FileStorageException {
                deleteFile(space, fileId);
                addFile(space, namespace, logName, new DefaultFileUploadProcessor(false), new SequenceInputStream(logFile, in));
            }
        };
        processFileContent(new DefaultFileDownloadProcessor(space, fileId, fileProcessor));
    }

}
