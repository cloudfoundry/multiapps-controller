package com.sap.cloud.lm.sl.cf.persistence.services;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.sap.cloud.lm.sl.cf.persistence.DataSourceWithDialect;
import com.sap.cloud.lm.sl.cf.persistence.executors.SqlQueryExecutor;
import com.sap.cloud.lm.sl.cf.persistence.message.Messages;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage.ProgressMessageType;
import com.sap.cloud.lm.sl.cf.persistence.query.providers.SqlProgressMessageQueryProvider;
import com.sap.cloud.lm.sl.common.SLException;

@Named
public class ProgressMessageService {

    private static final String DEFAULT_TABLE_NAME = "PROGRESS_MESSAGE";

    private final SqlQueryExecutor sqlQueryExecutor;
    private final SqlProgressMessageQueryProvider sqlProgressMessageQueryProvider;

    @Inject
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
            throw new SLException(e, Messages.ERROR_SAVING_MESSAGE, message.getProcessId(), message.getTaskId());
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

    public int removeOlderThan(Date timestamp) {
        try {
            return getSqlQueryExecutor().execute(getSqlProgressMessageQueryProvider().getRemoveOlderThanQuery(timestamp));
        } catch (SQLException e) {
            throw new SLException(e, Messages.ERROR_DELETING_MESSAGES_OLDER_THAN, timestamp);
        }
    }

    public int removeByProcessInstanceIdAndTaskIdAndType(final String processId, String taskId, ProgressMessageType progressMessageType) {
        try {
            return getSqlQueryExecutor().execute(getSqlProgressMessageQueryProvider().getRemoveByProcessInstanceIdAndTaskIdAndTypeQuery(processId,
                                                                                                                                        taskId,
                                                                                                                                        progressMessageType));
        } catch (SQLException e) {
            throw new SLException(e, Messages.ERROR_DELETING_MESSAGES_FOR_PROCESS_ID_AND_TASK_ID, processId, taskId);
        }
    }

    public List<ProgressMessage> findByProcessId(final String processId) {
        try {
            return getSqlQueryExecutor().execute(getSqlProgressMessageQueryProvider().getFindByProcessIdQuery(processId));
        } catch (SQLException e) {
            throw new SLException(e, Messages.ERROR_GETTING_MESSAGES_WITH_PROCESS_ID, processId);
        }
    }

    protected SqlQueryExecutor getSqlQueryExecutor() {
        return sqlQueryExecutor;
    }

    protected SqlProgressMessageQueryProvider getSqlProgressMessageQueryProvider() {
        return sqlProgressMessageQueryProvider;
    }

}
