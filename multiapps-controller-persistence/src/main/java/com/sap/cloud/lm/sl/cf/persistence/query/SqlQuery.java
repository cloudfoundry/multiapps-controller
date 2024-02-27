package com.sap.cloud.lm.sl.cf.persistence.query;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface SqlQuery<R> {

    public R execute(Connection connection) throws SQLException;
}
