package com.sap.cloud.lm.sl.cf.persistence.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.persistence.DataSourceWithDialect;
import com.sap.cloud.lm.sl.cf.persistence.message.Messages;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.model.FileInfo;
import com.sap.cloud.lm.sl.cf.persistence.processors.DefaultFileDownloadProcessor;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.DigestHelper;

public class ProcessLogsPersistenceService extends DatabaseFileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessLogsPersistenceService.class);

    private static final String DIGEST_METHOD = "MD5";
    private static final String TABLE_NAME = "process_log";

    public ProcessLogsPersistenceService(DataSourceWithDialect dataSourceWithDialect) {
        super(TABLE_NAME, dataSourceWithDialect);
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
        List<String> logIds = findAndSortFileIds(space, namespace, logName);

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

    private List<String> findAndSortFileIds(String space, String namespace, String fileName) throws FileStorageException {
        List<FileEntry> listFiles = listFiles(space, namespace);
        return listFiles.stream()
            .filter(file -> fileName.equals(file.getName()))
            .sorted(new Comparator<FileEntry>() {
                @Override
                public int compare(FileEntry arg0, FileEntry arg1) {
                    return arg0.getModified()
                        .compareTo(arg1.getModified());
                }
            })
            .map(file -> file.getId())
            .collect(Collectors.toList());
    }

    public void persistLog(String space, String namespace, File localLog, String remoteLogName) {
        try {
            storeLogFile(space, namespace, remoteLogName, localLog);
        } catch (FileStorageException e) {
            LOGGER.warn(MessageFormat.format(Messages.COULD_NOT_PERSIST_LOGS_FILE, localLog.getName()));
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
            return getSqlQueryExecutor().execute(getSqlFileQueryProvider().getDeleteProcessLogByNamespaceQuery(namespace));
        } catch (SQLException e) {
            throw new SLException(e, Messages.ERROR_DELETING_PROCESS_LOGS_WITH_NAMESPACE, namespace);
        }
    }
}
