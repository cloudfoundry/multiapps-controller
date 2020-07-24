package org.cloudfoundry.multiapps.controller.persistence.changes;

import java.sql.SQLException;

import javax.sql.DataSource;

public interface AsyncChange {

    void execute(DataSource dataSource) throws SQLException;

}
