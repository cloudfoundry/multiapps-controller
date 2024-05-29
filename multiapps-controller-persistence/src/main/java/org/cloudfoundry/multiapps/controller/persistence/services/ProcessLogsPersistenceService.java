package org.cloudfoundry.multiapps.controller.persistence.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.cloudfoundry.multiapps.common.util.DigestHelper;
import org.cloudfoundry.multiapps.controller.persistence.Constants;
import org.cloudfoundry.multiapps.controller.persistence.DataSourceWithDialect;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.FileInfo;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableFileEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableFileInfo;
import org.cloudfoundry.multiapps.controller.persistence.model.OperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.query.providers.ByteArraySqlFileQueryProvider;
import org.cloudfoundry.multiapps.controller.persistence.query.providers.SqlOperationLogQueryProvider;

@Named("processLogsPersistenceService")
public class ProcessLogsPersistenceService extends DatabaseFileService {

    public static final String TABLE_NAME = "process_log";
    private final SqlOperationLogQueryProvider sqlOperationLogQueryProvider;

    public ProcessLogsPersistenceService(DataSourceWithDialect dataSourceWithDialect) {
        super(dataSourceWithDialect, new ByteArraySqlFileQueryProvider(TABLE_NAME, dataSourceWithDialect.getDataSourceDialect()));
        sqlOperationLogQueryProvider = new SqlOperationLogQueryProvider(TABLE_NAME);
    }

    public List<String> getLogNamesBackwardsCompatible(String space, String operationId) throws FileStorageException {
        List<FileEntry> logFiles = listFilesBySpaceAndOperationId(space, operationId);
        return logFiles.stream()
                       .map(FileEntry::getName)
                       .distinct()
                       .collect(Collectors.toList());
    }

    public List<String> getLogNames(String space, String operationId) throws FileStorageException {
        List<OperationLogEntry> operationLogEntries = listOperationLogsBySpaceAndOperationId(space, operationId);
        return operationLogEntries.stream()
                                  .map(OperationLogEntry::getOperationLogName)
                                  .distinct()
                                  .filter(Objects::nonNull)
                                  .collect(Collectors.toList());
    }

    public List<OperationLogEntry> listOperationLogsBySpaceAndOperationId(String space, String operationId) throws FileStorageException {
        try {
            return getSqlQueryExecutor().execute(sqlOperationLogQueryProvider.getListFilesQueryBySpaceAndOperationId(space, operationId));
        } catch (SQLException e) {
            throw new FileStorageException(MessageFormat.format(Messages.ERROR_GETTING_LOGS_WITH_SPACE_AND_OPERATION_ID, space,
                                                                operationId),
                                           e);
        }
    }

    public String getLogContent(String space, String operationId, String logName) throws FileStorageException {
        List<FileEntry> logFiles = listFiles(space, operationId, logName);

        StringBuilder builder = new StringBuilder();
        for (FileEntry file : logFiles) {
            String content = processFileContent(space, file.getId(), inputStream -> IOUtils.toString(inputStream, StandardCharsets.UTF_8));
            builder.append(content);
        }
        return builder.toString();
    }

    public String getOperationLog(String space, String operationId, String logId) throws FileStorageException {
        List<OperationLogEntry> operationLogs = listOperationLogs(space, operationId, logId);

        StringBuilder builder = new StringBuilder();
        for (OperationLogEntry operationLog : operationLogs) {
            builder.append(operationLog.getOperationLog());
        }
        return builder.toString();
    }

    private List<FileEntry> listFiles(final String space, final String operationId, final String fileName) throws FileStorageException {
        try {
            return getSqlQueryExecutor().execute(getSqlFileQueryProvider().getListFilesQueryBySpaceOperationIdAndFileName(space,
                                                                                                                          operationId,
                                                                                                                          fileName));
        } catch (SQLException e) {
            throw new FileStorageException(MessageFormat.format(Messages.ERROR_GETTING_FILES_WITH_SPACE_OPERATION_ID_AND_NAME, space,
                                                                operationId, fileName),
                                           e);
        }
    }

    private List<OperationLogEntry> listOperationLogs(final String space, final String operationId, String logId)
        throws FileStorageException {
        try {
            return getSqlQueryExecutor().execute(sqlOperationLogQueryProvider.getListFilesQueryBySpaceOperationIdAndLogId(space,
                                                                                                                          operationId,
                                                                                                                          logId));
        } catch (SQLException e) {
            throw new FileStorageException(MessageFormat.format(Messages.ERROR_GETTING_LOGS_WITH_SPACE_OPERATION_ID_AND_NAME, space,
                                                                operationId, logId),
                                           e);
        }
    }

    public void persistLog(OperationLogEntry operationLogEntry) {
        try {
            getSqlQueryExecutor().execute(sqlOperationLogQueryProvider.getStoreLogQuery(operationLogEntry));
        } catch (SQLException e) {
            throw new OperationLogStorageException(Messages.FAILED_TO_SAVE_OPERATION_LOG_IN_DATABASE, e);
        }
    }
}
