package com.sap.cloud.lm.sl.cf.persistence.changes;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.MessageFormat;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.persistence.message.Messages;
import com.sap.cloud.lm.sl.cf.persistence.services.SqlExecutor;
import com.sap.cloud.lm.sl.cf.persistence.services.SqlExecutor.StatementExecutor;
import com.sap.cloud.lm.sl.cf.persistence.util.JdbcUtil;

public abstract class AbstractIndexSQLChange implements AsyncChange {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractIndexSQLChange.class);

    @Override
    public void execute(DataSource dataSource) throws SQLException {
        SqlExecutor executor = new SqlExecutor(dataSource);
        executor.execute(new StatementExecutor<Void>() {

            @Override
            public Void execute(Connection connection) throws SQLException {
                PreparedStatement statement = null;
                try {
                    LOGGER.info(MessageFormat.format(Messages.CREATING_INDEX_CONCURRENTLY, getIndexName()));
                    statement = connection.prepareStatement(getQuery());
                    statement.executeUpdate();
                    LOGGER.info(Messages.INDEX_CREATED);
                } finally {
                    JdbcUtil.closeQuietly(statement);
                }
                return null;
            }

        });
    }

    protected abstract String getQuery();

    protected abstract String getIndexName();
}
