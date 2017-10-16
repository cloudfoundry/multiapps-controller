package com.sap.cloud.lm.sl.cf.core.liquibase;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil;

import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;

public class SplitTargetSpaceColumn extends AbstractDataTransformationChange {

    private static final String TABLE_NAME = "CONFIGURATION_REGISTRY";
    private static final String SEARCH_QUERY = "Select ID, TARGET_SPACE from CONFIGURATION_REGISTRY";
    private static final String UPDATE_QUERY = "UPDATE CONFIGURATION_REGISTRY SET TARGET_ORG=?, TARGET_SPACE=? WHERE ID=?";

    @Override
    public Map<Long, String> customExtractData(ResultSet query) throws CustomChangeException, DatabaseException, SQLException {

        Map<Long, String> result = new HashMap<Long, String>();
        while (query.next()) {
            long id = query.getLong("ID");
            String targetSpace = query.getString("TARGET_SPACE");
            result.put(id, targetSpace);
            logger.debug(String.format("Retrieve data from row ID: '%s' and TARGET_SPACE: '%s'", id, targetSpace));
        }
        return result;
    }

    @Override
    public void customUpdate(PreparedStatement preparedStatement, Entry<Long, String> entry) throws SQLException {

        CloudTarget cloudTarget = ConfigurationEntriesUtil.splitTargetSpaceValue(entry.getValue());
        preparedStatement.setString(1, cloudTarget.getOrg());
        preparedStatement.setString(2, cloudTarget.getSpace());
        preparedStatement.setLong(3, entry.getKey());

        preparedStatement.addBatch();
        logger.debug(String.format("Executed update for row ID: '%s' , TARGET_ORG: '%s' , TARGET_SPACE: '%s'", entry.getKey(),
            cloudTarget.getOrg(), cloudTarget.getSpace()));
    }

    @Override
    public String getSearchQuery() {
        return SEARCH_QUERY;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public Map<Long, String> transformData(Map<Long, String> retrievedData) {
        return retrievedData;
    }

    @Override
    public String getUpdateQuery() {
        return UPDATE_QUERY;
    }

    @Override
    public String getConfirmationMessage() {
        return Messages.SPLIT_TARGET_SPACE_COLUMN;
    }
}
