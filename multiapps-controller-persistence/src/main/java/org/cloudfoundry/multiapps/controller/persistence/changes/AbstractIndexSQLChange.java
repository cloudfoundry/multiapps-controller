package org.cloudfoundry.multiapps.controller.persistence.changes;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.MessageFormat;

import javax.sql.DataSource;

import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.query.SqlQuery;
import org.cloudfoundry.multiapps.controller.persistence.util.JdbcUtil;
import org.cloudfoundry.multiapps.controller.persistence.util.SqlQueryExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractIndexSQLChange implements AsyncChange {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractIndexSQLChange.class);

    @Override
    public void execute(DataSource dataSource) throws SQLException {
        new SqlQueryExecutor(dataSource).executeWithAutoCommit(getIndexChangeQuery());
    }

    private SqlQuery<Void> getIndexChangeQuery() {
        return (Connection connection) -> {
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
        };
    }

    protected abstract String getQuery();

    protected abstract String getIndexName();
}
