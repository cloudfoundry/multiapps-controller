package org.cloudfoundry.multiapps.controller.persistence.query.providers;

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cloudfoundry.multiapps.controller.persistence.dialects.DataSourceDialect;
import org.cloudfoundry.multiapps.controller.persistence.query.options.StreamFetchingOptions;

public class BlobSqlFileQueryProvider extends SqlFileQueryProvider {

    public BlobSqlFileQueryProvider(String tableName, DataSourceDialect dataSourceDialect) {
        super(tableName, dataSourceDialect);
    }

    @Override
    protected void setContentBinaryStream(PreparedStatement statement, int index, InputStream content) throws SQLException {
        getDataSourceDialect().setBlobAsBinaryStream(statement, index, content);
    }

    @Override
    protected InputStream getContentBinaryStream(ResultSet resultSet, String columnName) throws SQLException {
        return getDataSourceDialect().getBinaryStreamFromBlob(resultSet, columnName);
    }

    @Override
    protected InputStream getContentBinaryStreamWithOffset(ResultSet resultSet, String columnName,
                                                           StreamFetchingOptions streamFetchingOptions)
        throws SQLException {
        return getDataSourceDialect().getBinaryStreamFromBlob(resultSet, columnName, streamFetchingOptions);
    }
}
