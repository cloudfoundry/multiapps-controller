package com.sap.cloud.lm.sl.cf.persistence.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;

import com.sap.cloud.lm.sl.cf.persistence.DataSourceWithDialect;
import com.sap.cloud.lm.sl.cf.persistence.dialects.DataSourceDialect;
import com.sap.cloud.lm.sl.cf.persistence.message.Messages;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.model.FileInfo;
import com.sap.cloud.lm.sl.cf.persistence.processors.DefaultFileDownloadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.query.providers.BlobSqlFileQueryProvider;
import com.sap.cloud.lm.sl.cf.persistence.query.providers.ByteArraySqlFileQueryProvider;
import com.sap.cloud.lm.sl.cf.persistence.query.providers.SqlFileQueryProvider;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.DigestHelper;

public class ProcessLogsPersistenceService extends DatabaseFileService {

    private static final String DIGEST_METHOD = "MD5";
    public static final String TABLE_NAME = "process_log";

    public ProcessLogsPersistenceService(DataSourceWithDialect dataSourceWithDialect, boolean isContentStoredAsBlob) {
        super(dataSourceWithDialect,
            createSqlFileQueryProvider(TABLE_NAME, dataSourceWithDialect.getDataSourceDialect(), isContentStoredAsBlob));
    }

    private static SqlFileQueryProvider createSqlFileQueryProvider(String tableName, DataSourceDialect dataSourceDialect,
        boolean isContentStoredAsBlob) {
        return isContentStoredAsBlob ? new BlobSqlFileQueryProvider(tableName, dataSourceDialect)
            : new ByteArraySqlFileQueryProvider(tableName, dataSourceDialect);
    }

    public List<String> getLogNames(String space, String namespace) throws FileStorageException {
        List<FileEntry> logFiles = listFiles(space, namespace);
        return logFiles.stream()
            .map(entry -> entry.getName())
            .distinct()
            .collect(Collectors.toList());
    }

    public String getLogContent(String space, String namespace, String logName) throws FileStorageException {
        final StringBuilder builder = new StringBuilder();
        List<String> logIds = getSortedByTimestampFileIds(space, namespace, logName);

        FileContentProcessor streamProcessor = new FileContentProcessor() {
            @Override
            public void processFileContent(InputStream is) throws IOException {
                builder.append(IOUtils.toString(is));
            }
        };
        for (String logId : logIds) {
            processFileContent(new DefaultFileDownloadProcessor(space, logId, streamProcessor));
        }
        return builder.toString();
    }

    private List<String> getSortedByTimestampFileIds(String space, String namespace, String fileName) throws FileStorageException {
        List<FileEntry> listFiles = listFiles(space, namespace, fileName);
        return listFiles.stream()
            .sorted((FileEntry f1, FileEntry f2) -> f1.getModified()
                .compareTo(f2.getModified()))
            .map(file -> file.getId())
            .collect(Collectors.toList());
    }

    private List<FileEntry> listFiles(final String space, final String namespace, final String fileName) throws FileStorageException {
        try {
            return getSqlQueryExecutor().execute(getSqlFileQueryProvider().getListFilesQuery(space, namespace, fileName));
        } catch (SQLException e) {
            throw new FileStorageException(
                MessageFormat.format(Messages.ERROR_GETTING_FILES_WITH_SPACE_NAMESPACE_AND_NAME, space, namespace, fileName), e);
        }
    }

    public void persistLog(String space, String namespace, File localLog, String remoteLogName) {
        try {
            storeLogFile(space, namespace, remoteLogName, localLog);
        } catch (FileStorageException e) {
            logger.warn(MessageFormat.format(Messages.COULD_NOT_PERSIST_LOGS_FILE, localLog.getName()));
        }
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

    public int deleteByNamespace(final String namespace) {
        try {
            return getSqlQueryExecutor().execute(getSqlFileQueryProvider().getDeleteByNamespaceQuery(namespace));
        } catch (SQLException e) {
            throw new SLException(e, Messages.ERROR_DELETING_PROCESS_LOGS_WITH_NAMESPACE, namespace);
        }
    }
}
