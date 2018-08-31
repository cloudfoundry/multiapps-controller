package com.sap.cloud.lm.sl.cf.persistence.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.sap.cloud.lm.sl.cf.persistence.DataSourceWithDialect;
import com.sap.cloud.lm.sl.cf.persistence.message.Messages;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.processors.DefaultFileDownloadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.processors.DefaultFileUploadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.services.SqlExecutor.StatementExecutor;
import com.sap.cloud.lm.sl.cf.persistence.util.JdbcUtil;
import com.sap.cloud.lm.sl.common.SLException;

public class ProcessLogsPersistenceService extends DatabaseFileService {

    private static final String LOG_FILE_EXTENSION = ".log";
    private static final String TABLE_NAME = "process_log";
    private static final String DEFAULT_SPACE = "DEFAULT";
    private static final String DELETE_CONTENT_BY_NAMESPACE = "DELETE FROM %s WHERE NAMESPACE=?";

    public ProcessLogsPersistenceService(DataSourceWithDialect dataSourceWithDialect) {
        super(TABLE_NAME, dataSourceWithDialect);
    }

    public List<String> getLogNames(String namespace) throws FileStorageException {
        return getLogNames(DEFAULT_SPACE, namespace);
    }

    public List<String> getLogNames(String space, String namespace) throws FileStorageException {
        List<String> result = new ArrayList<>();
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
        return rowsDeleted > 0;
    }

    public String findFileId(String namespace, String fileName) throws FileStorageException {
        return findFileId(DEFAULT_SPACE, namespace, fileName);
    }

    public String findFileId(String space, String namespace, String fileName) throws FileStorageException {
        List<FileEntry> listFiles = listFiles(space, namespace);
        for (FileEntry fileEntry : listFiles) {
            if (fileEntry.getName()
                .equals(fileName)) {
                return fileEntry.getId();
            }
        }
        return null;
    }

    private static String getFileName(String logId, String name) {
        return new StringBuilder(logId).append(".")
            .append(name)
            .append(LOG_FILE_EXTENSION)
            .toString();
    }

    public void appendLog(String namespace, String logName, String logDir) throws IOException, FileStorageException {
        appendLog(DEFAULT_SPACE, namespace, logName, logDir);
    }

    public void appendLog(String space, String namespace, String logName, String logDir) throws IOException, FileStorageException {
        try {
            getSqlExecutor().executeInSingleTransaction(new StatementExecutor<Void>() {
                @Override
                public Void execute(Connection connection) throws SQLException {
                    File logFile = getFile(namespace, logName, logDir);
                    // we have no concerns of DOS attack, the files are coming from our system

                    InputStream in = null;
                    try {
                        in = new FileInputStream(logFile);
                        appendLog(space, namespace, logName, in);
                    } catch (FileNotFoundException e) {
                        throw new SQLException(MessageFormat.format(Messages.FILE_NOT_FOUND, getFileName(namespace, logName)));
                    } catch (FileStorageException e) {
                        throw new SQLException(e);
                    } finally {
                        IOUtils.closeQuietly(in);
                    }
                    return null;
                }

            });
        } catch (SQLException e) {
            throw new FileStorageException(e.getMessage(), e);
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

    public int deleteByNamespace(final String namespace) {
        try {
            return getSqlExecutor().executeInSingleTransaction(new StatementExecutor<Integer>() {
                @Override
                public Integer execute(Connection connection) throws SQLException {
                    PreparedStatement statement = null;
                    try {
                        statement = connection.prepareStatement(getQuery(DELETE_CONTENT_BY_NAMESPACE));
                        statement.setString(1, namespace);
                        return statement.executeUpdate();
                    } finally {
                        JdbcUtil.closeQuietly(statement);
                    }
                }
            });
        } catch (SQLException e) {
            throw new SLException(e, Messages.ERROR_DELETING_PROCESS_LOGS_WITH_NAMESPACE, namespace);
        }
    }

    @Override
    protected int deleteFilesWithoutContent() throws FileStorageException {
        // Files without content should only exist when switching from one implementation to another (FileSystemFileService ->
        // DatabaseFileService, for example). There is no alternative implementation of ProcessLogsPersistenceService, so there shouldn't be
        // any files without content.
        return 0;
    }

    @Override
    protected void logFileEntry(FileEntry fileEntry) {
        // Do not log MAIN_LOG file entry
    }

}
