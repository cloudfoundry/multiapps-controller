package com.sap.cloud.lm.sl.cf.persistence.dialects;

import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PostgreSQLDataSourceDialect extends DefaultDataSourceDialect implements DataSourceDialect {

    @Override
    public String getSequenceNextValueSyntax(String sequenceName) {
        return "nextval('" + sequenceName + "')";
    }

    @Override
    public void setBigInteger(PreparedStatement ps, int index, BigInteger bi) throws SQLException {
        ps.setLong(index, bi.longValue());
    }

}
