package org.cloudfoundry.multiapps.controller.database.migration.executor.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class BytesDatabaseTypeSetter implements DatabaseTypeSetter {

    private static final String BYTEA = "bytea";

    @Override
    public List<String> getSupportedTypes() {
        return List.of(BYTEA);
    }

    @Override
    public void setType(int columnIndex, PreparedStatement insertStatement, Object value) throws SQLException {
        insertStatement.setBytes(columnIndex, (byte[]) value);
    }

}
