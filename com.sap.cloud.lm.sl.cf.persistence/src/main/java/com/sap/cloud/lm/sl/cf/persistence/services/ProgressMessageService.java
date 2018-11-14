package com.sap.cloud.lm.sl.cf.persistence.services;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import com.sap.cloud.lm.sl.cf.persistence.DataSourceWithDialect;
import com.sap.cloud.lm.sl.cf.persistence.executors.SqlQueryExecutor;
import com.sap.cloud.lm.sl.cf.persistence.message.Messages;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage.ProgressMessageType;
import com.sap.cloud.lm.sl.cf.persistence.query.providers.SqlProgressMessageQueryProvider;
import com.sap.cloud.lm.sl.common.SLException;

public class ProgressMessageService {

    private static final String DEFAULT_TABLE_NAME = "PROGRESS_MESSAGE";

    private final SqlQueryExecutor sqlQueryExecutor;
    private final SqlProgressMessageQueryProvider sqlProgressMessageQueryProvider;

    public ProgressMessageService(DataSourceWithDialect dataSourceWithDialect) {
        this(DEFAULT_TABLE_NAME, dataSourceWithDialect);
    }

    protected ProgressMessageService(String tableName, DataSourceWithDialect dataSourceWithDialect) {
        this.sqlQueryExecutor = new SqlQueryExecutor(dataSourceWithDialect.getDataSource());
        this.sqlProgressMessageQueryProvider = new SqlProgressMessageQueryProvider(tableName, dataSourceWithDialect);
    }

    public boolean add(final ProgressMessage message) {
        try {
            return getSqlQueryExecutor().execute(getSqlProgressMessageQueryProvider().getAddQuery(message));
        } catch (SQLException e) {
            throw new SLException(e, Messages.ERROR_SAVING_MESSAGE, message.getProcessId(), message.getTaskId(),
                message.getTaskExecutionId());
        }
    }

    public boolean update(final long existingId, final ProgressMessage newMessage) {
        try {
            return getSqlQueryExecutor().execute(getSqlProgressMessageQueryProvider().getUpdateQuery(existingId, newMessage));
        } catch (SQLException e) {
            throw new SLException(e, Messages.ERROR_UPDATING_MESSAGE, newMessage.getId());
        }
    }

    public int removeByProcessId(final String processId) {
        try {
            return getSqlQueryExecutor().execute(getSqlProgressMessageQueryProvider().getRemoveByProcessIdQuery(processId));
        } catch (SQLException e) {
            throw new SLException(e, Messages.ERROR_DELETING_MESSAGES_WITH_PROCESS_ID, processId);
        }
    }

    public int removeByProcessIdTaskIdAndTaskExecutionId(final String processId, final String taskId, final String taskExecutionId) {
        try {
            return getSqlQueryExecutor().execute(
                getSqlProgressMessageQueryProvider().getRemoveByProcessIdTaskIdAndTaskExecutionIdQuery(processId, taskId, taskExecutionId));
        } catch (SQLException e) {
            throw new SLException(e, Messages.ERROR_DELETING_MESSAGES_WITH_PROCESS_ID_TASK_ID_AND_TASK_EXECUTION_ID, processId, taskId,
                taskExecutionId);
        }
    }

    public int removeOlderThan(Date timestamp) {
        try {
            return getSqlQueryExecutor().execute(getSqlProgressMessageQueryProvider().getRemoveOlderThanQuery(timestamp));
        } catch (SQLException e) {
            throw new SLException(e, Messages.ERROR_DELETING_MESSAGES_OLDER_THAN, timestamp);
        }
    }

    public int removeByProcessInstanceIdAndTaskId(final String processId, String taskId) {
        try {
            return getSqlQueryExecutor()
                .execute(getSqlProgressMessageQueryProvider().getRemoveByProcessInstanceIdAndTaskIdQuery(processId, taskId));
        } catch (SQLException e) {
            throw new SLException(e, Messages.ERROR_DELETING_MESSAGES_FOR_PROCESS_ID_AND_TASK_ID, processId, taskId);
        }
    }

    public List<ProgressMessage> findAll() {
        try {
            return getSqlQueryExecutor().execute(getSqlProgressMessageQueryProvider().getFindAllQuery());
        } catch (SQLException e) {
            throw new SLException(e, Messages.ERROR_GETTING_ALL_MESSAGES);
        }
    }

    public List<ProgressMessage> findByProcessId(final String processId) {
        try {
            return getSqlQueryExecutor().execute(getSqlProgressMessageQueryProvider().getFindByProcessIdQuery(processId));
        } catch (SQLException e) {
            throw new SLException(e, Messages.ERROR_GETTING_MESSAGES_WITH_PROCESS_ID, processId);
        }
    }

    public List<ProgressMessage> findByProcessIdTaskIdAndTaskExecutionId(final String processId, final String taskId,
        final String taskExecutionId) {
        try {
            return getSqlQueryExecutor().execute(
                getSqlProgressMessageQueryProvider().getFindByProcessIdTaskIdAndTaskExecutionIdQuery(processId, taskId, taskExecutionId));
        } catch (SQLException e) {
            throw new SLException(e, Messages.ERROR_GETTING_MESSAGES_WITH_PROCESS_ID_TASK_ID_AND_TASK_EXECUTION_ID, processId, taskId,
                taskExecutionId);
        }
    }

    public String findLastTaskExecutionId(final String processId, final String taskId) {
        try {
            return getSqlQueryExecutor().execute(getSqlProgressMessageQueryProvider().getFindLastTaskExecutionIdQuery(processId, taskId));
        } catch (SQLException e) {
            throw new SLException(e, Messages.ERROR_GETTING_LAST_MESSAGE_WITH_PROCESS_ID_AND_TASK_ID, processId, taskId);
        }
    }

    public List<ProgressMessage> findByProcessIdTaskIdTaskExecutionIdAndType(final String processId, final String taskId,
        final String taskExecutionId, final ProgressMessageType type) {
        try {
            return getSqlQueryExecutor().execute(getSqlProgressMessageQueryProvider()
                .getFindByProcessIdTaskIdTaskExecutionIdAndTypeQuery(processId, taskId, taskExecutionId, type));
        } catch (SQLException e) {
            throw new SLException(e, Messages.ERROR_GETTING_MESSAGES_WITH_PROCESS_ID_TASK_ID_TASK_EXECUTION_ID_AND_TYPE, processId, taskId,
                taskExecutionId, type.name());
        }
    }

    public List<ProgressMessage> findByProcessIdAndType(final String processInstanceId, final ProgressMessageType type) {
        try {
            return getSqlQueryExecutor()
                .execute(getSqlProgressMessageQueryProvider().getFindByProcessidAndTypeQuery(processInstanceId, type));
        } catch (SQLException e) {
            throw new SLException(e, Messages.ERROR_GETTING_MESSAGES_WITH_PROCESS_ID_AND_TYPE, processInstanceId, type.name());
        }
    }

    protected SqlQueryExecutor getSqlQueryExecutor() {
        return sqlQueryExecutor;
    }

    protected SqlProgressMessageQueryProvider getSqlProgressMessageQueryProvider() {
        return sqlProgressMessageQueryProvider;
    }

}
