package org.cloudfoundry.multiapps.controller.persistence.stream;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.io.IOUtils;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.persistence.util.JdbcUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBInputStream extends FilterInputStream { // NOSONAR does not need to override read(byte b[], int off, int len)

    private static final Logger LOGGER = LoggerFactory.getLogger(DBInputStream.class);

    private final PreparedStatement statement;
    private final ResultSet resultSet;
    private final Connection connection;

    public DBInputStream(InputStream inputStream, PreparedStatement statement, ResultSet resultSet, Connection connection) {
        super(inputStream);
        this.statement = statement;
        this.resultSet = resultSet;
        this.connection = connection;
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(in, e -> LOGGER.warn(e.getMessage(), e));
        JdbcUtil.closeQuietly(statement);
        JdbcUtil.closeQuietly(resultSet);
        try {
            JdbcUtil.commit(connection);
        } catch (SQLException e) {
            throw new SLException(e.getMessage(), e);
        } finally {
            setAutoCommit();
            JdbcUtil.closeQuietly(connection);
        }
    }

    private void setAutoCommit() {
        try {
            JdbcUtil.setAutoCommitSafely(connection);
        } catch (SQLException e) {
            throw new SLException(e.getMessage(), e);
        }
    }
}
