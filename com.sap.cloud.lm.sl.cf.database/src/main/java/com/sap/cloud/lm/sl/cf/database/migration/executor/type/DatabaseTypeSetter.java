package com.sap.cloud.lm.sl.cf.database.migration.executor.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public interface DatabaseTypeSetter {

    List<String> getSupportedTypes();

    void setType(int columnIndex, PreparedStatement insertStatement, ResultSet sourceData) throws SQLException;
}
