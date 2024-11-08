package org.cloudfoundry.multiapps.controller.persistence.dialects;

import java.io.InputStream;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cloudfoundry.multiapps.controller.persistence.query.options.StreamFetchingOptions;

public class DefaultDataSourceDialect implements DataSourceDialect {

    @Override
    public String getSequenceNextValueSyntax(String sequenceName) {
        return "nextval('" + sequenceName + "')";
    }

    @Override
    public InputStream getBinaryStreamFromBlob(ResultSet rs, String columnName) throws SQLException {
        return rs.getBlob(columnName)
                 .getBinaryStream();
    }

    @Override
    public InputStream getBinaryStreamFromBlob(ResultSet rs, String columnName, StreamFetchingOptions streamFetchingOptions)
        throws SQLException {
        return rs.getBlob(columnName)
                 // + 1 is required as the first position is 1 instead of 0
                 // pos – the offset to the first byte of the partial value to be retrieved. The first byte in the Blob is at
                 // position 1.
                 .getBinaryStream(streamFetchingOptions.startOffset() + 1,
                                  streamFetchingOptions.endOffset() - streamFetchingOptions.startOffset() + 1);
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
        ps.setLong(index, bi.longValue());
    }

    @Override
    public InputStream getBinaryStreamFromByteArray(ResultSet rs, String columnName) throws SQLException {
        return rs.getBinaryStream(columnName);
    }

    @Override
    public void setByteArrayAsBinaryStream(PreparedStatement ps, int index, InputStream is) throws SQLException {
        ps.setBinaryStream(index, is);
    }

}
