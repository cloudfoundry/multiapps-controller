package com.sap.cloud.lm.sl.cf.persistence.query.providers;

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.sap.cloud.lm.sl.cf.persistence.dialects.DataSourceDialect;

public class ExternalSqlFileQueryProvider extends SqlFileQueryProvider {

    public ExternalSqlFileQueryProvider(String tableName, DataSourceDialect dataSourceDialect) {
        super(tableName, dataSourceDialect);
    }

    @Override
    protected void setContentBinaryStream(PreparedStatement statement, int index, InputStream content) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected InputStream getContentBinaryStream(ResultSet resultSet, String columnName) {
        throw new UnsupportedOperationException();
    }

}
