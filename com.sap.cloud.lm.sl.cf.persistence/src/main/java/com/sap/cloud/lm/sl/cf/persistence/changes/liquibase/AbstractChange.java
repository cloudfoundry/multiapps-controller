package com.sap.cloud.lm.sl.cf.persistence.changes.liquibase;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.persistence.message.Messages;
import com.sap.cloud.lm.sl.cf.persistence.util.JdbcUtil;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

public abstract class AbstractChange implements CustomTaskChange {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
    }

    @Override
    public void setUp() {
    }

    @Override
    public ValidationErrors validate(Database database) {
        return null;
    }

    @Override
    public void execute(Database database) throws CustomChangeException {
        JdbcConnection jdbcConnection = (JdbcConnection) database.getConnection();
        try {
            execute(jdbcConnection);
        } catch (SQLException e) {
            JdbcUtil.logSQLException(e);
            throw new CustomChangeException(e);
        } catch (Exception e) {
            throw new CustomChangeException(e);
        }
    }

    protected void execute(JdbcConnection jdbcConnection) throws Exception {
        try {
            jdbcConnection.setAutoCommit(false);
            executeInTransaction(jdbcConnection);
            jdbcConnection.commit();
        } catch (Exception e) {
            attemptToRollbackTransaction(jdbcConnection);
            throw e;
        } finally {
            try {
                jdbcConnection.setAutoCommit(true);
            } catch (DatabaseException e) {
                logger.warn("Could not re-enable auto-commit.", e);
            }
        }
    }

    protected void executeInTransaction(JdbcConnection jdbcConnection) throws Exception {
    }

    private void attemptToRollbackTransaction(JdbcConnection jdbcConnection) {
        try {
            jdbcConnection.rollback();
        } catch (DatabaseException e) {
            logger.warn(Messages.COULD_NOT_ROLLBACK_TRANSACTION, e);
        }
    }

}
