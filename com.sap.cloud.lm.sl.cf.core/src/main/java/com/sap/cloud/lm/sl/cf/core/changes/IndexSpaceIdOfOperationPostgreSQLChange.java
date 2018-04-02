package com.sap.cloud.lm.sl.cf.core.changes;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.persistence.changes.AsyncChange;
import com.sap.cloud.lm.sl.persistence.changes.IndexProcessIdsOfProgressMessagesPostgreSQLChange;
import com.sap.cloud.lm.sl.persistence.services.SqlExecutor;
import com.sap.cloud.lm.sl.persistence.services.SqlExecutor.StatementExecutor;
import com.sap.cloud.lm.sl.persistence.util.JdbcUtil;

public class IndexSpaceIdOfOperationPostgreSQLChange implements AsyncChange {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexProcessIdsOfProgressMessagesPostgreSQLChange.class);
    private static final String CREATE_INDEX = "CREATE INDEX CONCURRENTLY IDX_OPERATION_SPACE_ID ON OPERATION(SPACE_ID)";

    @Override
    public void execute(DataSource dataSource) throws SQLException {
        SqlExecutor executor = new SqlExecutor(dataSource);
        executor.execute(new StatementExecutor<Void>() {

            @Override
            public Void execute(Connection connection) throws SQLException {
                PreparedStatement statement = null;
                try {
                    LOGGER.info("Processing index for column space_id of operation");
                    statement = connection.prepareStatement(CREATE_INDEX);
                    statement.executeUpdate();
                    LOGGER.info("Space_id column indexed");
                } finally {
                    JdbcUtil.closeQuietly(statement);
                }
                return null;
            }

        });
    }
}
