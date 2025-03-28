package org.cloudfoundry.multiapps.controller.persistence.services;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.persistence.DataSourceWithDialect;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.model.OperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.query.providers.ByteArraySqlFileQueryProvider;
import org.cloudfoundry.multiapps.controller.persistence.query.providers.SqlOperationLogQueryProvider;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Named("processLogsPersistenceService")
public class ProcessLogsPersistenceService extends DatabaseFileService {

    public static final String TABLE_NAME = "process_log";
    private final SqlOperationLogQueryProvider sqlOperationLogQueryProvider;

    public ProcessLogsPersistenceService(DataSourceWithDialect dataSourceWithDialect) {
        super(dataSourceWithDialect, new ByteArraySqlFileQueryProvider(TABLE_NAME, dataSourceWithDialect.getDataSourceDialect()));
        sqlOperationLogQueryProvider = new SqlOperationLogQueryProvider(TABLE_NAME);
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

    public String getOperationLog(String space, String operationId, String logId) throws FileStorageException {
        List<OperationLogEntry> operationLogs = listOperationLogs(space, operationId, logId);

        StringBuilder builder = new StringBuilder();
        for (OperationLogEntry operationLog : operationLogs) {
            builder.append(operationLog.getOperationLog());
        }
        return builder.toString();
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
