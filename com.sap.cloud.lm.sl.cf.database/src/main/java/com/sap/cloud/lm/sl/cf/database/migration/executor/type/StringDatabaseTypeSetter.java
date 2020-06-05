package com.sap.cloud.lm.sl.cf.database.migration.executor.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class StringDatabaseTypeSetter implements DatabaseTypeSetter {

    @Override
    public List<String> getSupportedTypes() {
        return Arrays.asList("varchar", "text");
    }

    @Override
    public void setType(int columnIndex, PreparedStatement insertStatement, ResultSet sourceData) throws SQLException {
        insertStatement.setString(columnIndex, sourceData.getString(columnIndex));
    }

}
