package org.cloudfoundry.multiapps.controller.persistence.query.providers;

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.cloudfoundry.multiapps.controller.persistence.dialects.DataSourceDialect;
import org.cloudfoundry.multiapps.controller.persistence.query.options.StreamFetchingOptions;

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

    @Override
    protected InputStream getContentBinaryStreamWithOffset(ResultSet resultSet, String columnName,
                                                           StreamFetchingOptions streamFetchingOptions) {
        throw new UnsupportedOperationException();
    }
}
