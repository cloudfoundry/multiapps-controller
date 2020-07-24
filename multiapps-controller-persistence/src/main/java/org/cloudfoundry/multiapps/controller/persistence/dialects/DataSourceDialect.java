package org.cloudfoundry.multiapps.controller.persistence.dialects;

import java.io.InputStream;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface DataSourceDialect {

    String getSequenceNextValueSyntax(String sequenceName);

    InputStream getBinaryStreamFromBlob(ResultSet rs, String columnName) throws SQLException;

    void setBlobAsBinaryStream(PreparedStatement ps, int index, InputStream is) throws SQLException;

    InputStream getBinaryStreamFromByteArray(ResultSet rs, String columnName) throws SQLException;

    void setByteArrayAsBinaryStream(PreparedStatement ps, int index, InputStream is) throws SQLException;

    BigInteger getBigInteger(ResultSet rs, String columnName) throws SQLException;

    void setBigInteger(PreparedStatement ps, int index, BigInteger bi) throws SQLException;
}
