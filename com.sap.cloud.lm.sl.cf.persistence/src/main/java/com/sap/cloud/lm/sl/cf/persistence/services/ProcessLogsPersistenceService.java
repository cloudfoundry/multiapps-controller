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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.persistence.DataSourceWithDialect;
import com.sap.cloud.lm.sl.cf.persistence.message.Messages;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.processors.DefaultFileDownloadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.processors.DefaultFileUploadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.services.SqlExecutor.StatementExecutor;
import com.sap.cloud.lm.sl.cf.persistence.util.JdbcUtil;
import com.sap.cloud.lm.sl.common.SLException;

public class ProcessLogsPersistenceService extends DatabaseFileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessLogsPersistenceService.class);

    private static final String DIGEST_METHOD = "MD5";
    private static final String TABLE_NAME = "process_log";

    public ProcessLogsPersistenceService(DataSourceWithDialect dataSourceWithDialect) {
        super(TABLE_NAME, dataSourceWithDialect);
    }

    public List<String> getLogNames(String space, String namespace) throws FileStorageException {
        List<String> result = new ArrayList<>();
        List<FileEntry> logFiles = listFiles(space, namespace);
        for (FileEntry logFile : logFiles) {
            result.add(logFile.getName());
        }
        return result;
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

    public boolean deleteLog(String space, String namespace, String logName) throws FileStorageException {
        String fileId = findFileId(space, namespace, logName);
        return deleteFile(space, fileId);
    }

    private String findFileId(String space, String namespace, String fileName) throws FileStorageException {
        List<FileEntry> listFiles = listFiles(space, namespace);
        for (FileEntry fileEntry : listFiles) {
            if (fileEntry.getName()
                .equals(fileName)) {
                return fileEntry.getId();
            }
        }
        return null;
    }

    public void appendLog(String space, String namespace, File localLog, String remoteLogName) {
        try {
            appendLog(space, namespace, remoteLogName, localLog);
        } catch (FileStorageException e) {
            LOGGER.warn(MessageFormat.format(Messages.COULD_NOT_PERSIST_LOGS_FILE, localLog.getName()));
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

}
