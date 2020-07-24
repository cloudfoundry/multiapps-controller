package com.sap.cloud.lm.sl.cf.persistence.query;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface SqlQuery<R> {

    R execute(Connection connection) throws SQLException;
}
