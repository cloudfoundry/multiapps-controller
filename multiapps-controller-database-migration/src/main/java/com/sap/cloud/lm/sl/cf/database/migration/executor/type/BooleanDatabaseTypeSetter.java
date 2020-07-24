package com.sap.cloud.lm.sl.cf.database.migration.executor.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class BooleanDatabaseTypeSetter implements DatabaseTypeSetter {

    @Override
    public List<String> getSupportedTypes() {
        return Arrays.asList("bool");
    }

    @Override
    public void setType(int columnIndex, PreparedStatement insertStatement, Object value) throws SQLException {
        insertStatement.setBoolean(columnIndex, (Boolean) value);
    }

}
