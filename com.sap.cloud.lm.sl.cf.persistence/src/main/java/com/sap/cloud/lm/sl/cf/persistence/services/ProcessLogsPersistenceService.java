package com.sap.cloud.lm.sl.cf.persistence.services;

import com.sap.cloud.lm.sl.cf.persistence.DataSourceWithDialect;
import com.sap.cloud.lm.sl.cf.persistence.message.Messages;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.model.FileInfo;
import com.sap.cloud.lm.sl.cf.persistence.processors.DefaultFileDownloadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.query.SqlQuery;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.DigestHelper;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

@Named("processLogsPersistenceService")
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

    private void appendLog(final String space, final String namespace, final String remoteLogName, File localLog)
            throws FileStorageException {
        final String fileId = findFileId(space, namespace, remoteLogName);

        if (fileId == null) {
            storeLogFile(space, namespace, remoteLogName, localLog);
            return;
        }

        FileContentProcessor fileProcessor = new FileContentProcessor() {
            @Override
            public void processFileContent(InputStream remoteLogStream) throws FileStorageException, FileNotFoundException {
                updateLogFile(space, fileId, localLog, remoteLogStream);
            }
        };
        processFileContent(new DefaultFileDownloadProcessor(space, fileId, fileProcessor));
    }

    private void storeLogFile(final String space, final String namespace, final String remoteLogName, File localLog)
            throws FileStorageException {
        try (InputStream inputStream = new FileInputStream(localLog)) {
            FileEntry localLogFileEntry = createFileEntry(space, namespace, remoteLogName, localLog);

            getSqlQueryExecutor().execute(getSqlFileQueryProvider().getStoreFileQuery(localLogFileEntry, inputStream));
        } catch (SQLException | IOException | NoSuchAlgorithmException e) {
            throw new FileStorageException(MessageFormat.format(Messages.ERROR_STORING_LOG_FILE, localLog.getName()), e);
        }
    }

    private FileEntry createFileEntry(final String space, final String namespace, final String remoteLogName, File localLog)
            throws NoSuchAlgorithmException, IOException {
        FileInfo localLogFileInfo = new FileInfo(localLog, BigInteger.valueOf(localLog.length()),
                DigestHelper.computeFileChecksum(localLog.toPath(), DIGEST_METHOD), DIGEST_METHOD);
        return createFileEntry(space, namespace, remoteLogName, localLogFileInfo);
    }

    private void updateLogFile(String space, String fileId, File localLog, InputStream remoteLogStream) throws FileStorageException {
        try (InputStream localLogStream = new FileInputStream(localLog);
             InputStream appendedLog = new SequenceInputStream(remoteLogStream, localLogStream)) {
            SqlQuery<Void> updateLogQuery = getUpdateLogQueries(space, fileId, localLog, appendedLog);
            getSqlQueryExecutor().execute(updateLogQuery);
        } catch (SQLException | IOException e) {
            throw new FileStorageException(MessageFormat.format(Messages.ERROR_UPDATING_LOG_FILE, localLog.getName()), e);
        }
    }

    private SqlQuery<Void> getUpdateLogQueries(String space, String fileId, File localLog, InputStream appendedLog) {
        return (Connection connection) -> {
            try {
                FileEntry dbFileEntry = getSqlFileQueryProvider().getRetrieveFileQuery(space, fileId)
                        .execute(connection);
                FileEntry newFileEntry = addSizeAndDigest(dbFileEntry, localLog);
                getSqlFileQueryProvider().getUpdateFileQuery(newFileEntry, appendedLog)
                        .execute(connection);
            } catch (NoSuchAlgorithmException e) {
                throw new SLException(Messages.ERROR_CALCULATING_FILE_DIGEST, localLog.getName(), e);
            } catch (IOException e) {
                throw new SLException(e);
            }
            return null;
        };
    }

    private FileEntry addSizeAndDigest(FileEntry dbFileEntry, File localLog) throws NoSuchAlgorithmException, IOException {
        dbFileEntry.setSize(dbFileEntry.getSize()
                .add(BigInteger.valueOf(localLog.length())));
        dbFileEntry.setDigest(DigestHelper.appendDigests(dbFileEntry.getDigest(),
                DigestHelper.computeFileChecksum(localLog.toPath(), dbFileEntry.getDigestAlgorithm()), dbFileEntry.getDigestAlgorithm()));
        dbFileEntry.setDigestAlgorithm(dbFileEntry.getDigestAlgorithm());
        dbFileEntry.setModified(new Timestamp(System.currentTimeMillis()));
        return dbFileEntry;
    }

    public int deleteByNamespace(final String namespace) {
        try {
            return getSqlQueryExecutor().execute(getSqlFileQueryProvider().getDeleteProcessLogByNamespaceQuery(namespace));
        } catch (SQLException e) {
            throw new SLException(e, Messages.ERROR_DELETING_PROCESS_LOGS_WITH_NAMESPACE, namespace);
        }
    }
}
