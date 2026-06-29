package org.cloudfoundry.multiapps.controller.database.migration.executor.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public class TimestampDatabaseTypeSetter implements DatabaseTypeSetter {

    private static final String TIMESTAMP = "timestamp";

    @Override
    public List<String> getSupportedTypes() {
        return List.of(TIMESTAMP);
    }

    @Override
    public void setType(int columnIndex, PreparedStatement insertStatement, Object value) throws SQLException {
        insertStatement.setTimestamp(columnIndex, (Timestamp) value);
    }

}
