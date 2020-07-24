package com.sap.cloud.lm.sl.cf.persistence.query.providers;

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.sap.cloud.lm.sl.cf.persistence.dialects.DataSourceDialect;

public class ByteArraySqlFileQueryProvider extends SqlFileQueryProvider {

    private static final String CONTENT_COLUMN_NAME = "CONTENT_BYTEA";

    public ByteArraySqlFileQueryProvider(String tableName, DataSourceDialect dataSourceDialect) {
        super(tableName, dataSourceDialect);
    }

    @Override
    protected String getContentColumnName() {
        return CONTENT_COLUMN_NAME;
    }

    @Override
    protected void setContentBinaryStream(PreparedStatement statement, int index, InputStream content) throws SQLException {
        getDataSourceDialect().setByteArrayAsBinaryStream(statement, index, content);
    }

    @Override
    protected InputStream getContentBinaryStream(ResultSet resultSet, String columnName) throws SQLException {
        return getDataSourceDialect().getBinaryStreamFromByteArray(resultSet, columnName);
    }

}
