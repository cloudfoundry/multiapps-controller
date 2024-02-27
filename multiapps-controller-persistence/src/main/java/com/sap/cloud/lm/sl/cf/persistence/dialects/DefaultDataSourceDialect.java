package com.sap.cloud.lm.sl.cf.persistence.dialects;

import java.io.InputStream;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DefaultDataSourceDialect implements DataSourceDialect {

    @Override
    public String getSequenceNextValueSyntax(String sequenceName) {
        return sequenceName + ".NEXTVAL";
    }

    @Override
    public InputStream getBinaryStreamFromBlob(ResultSet rs, String columnName) throws SQLException {
        return rs.getBlob(columnName)
                 .getBinaryStream();
    }

    @Override
    public void setBlobAsBinaryStream(PreparedStatement ps, int index, InputStream is) throws SQLException {
        ps.setBlob(index, is);
    }

    @Override
    public BigInteger getBigInteger(ResultSet rs, String columnName) throws SQLException {
        return new BigInteger(rs.getString(columnName));
    }

    @Override
    public void setBigInteger(PreparedStatement ps, int index, BigInteger bi) throws SQLException {
        ps.setString(index, bi.toString());
    }

}
