package com.sap.cloud.lm.sl.cf.core.liquibase;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil;
import com.sap.cloud.lm.sl.cf.persistence.changes.liquibase.AbstractDataTransformationChange;

public class SplitTargetSpaceColumn extends AbstractDataTransformationChange<Map<Long, String>, Map<Long, CloudTarget>> {

    private static final String SELECT_STATEMENT = "SELECT ID, TARGET_SPACE FROM CONFIGURATION_REGISTRY";
    private static final String UPDATE_STATEMENT = "UPDATE CONFIGURATION_REGISTRY SET TARGET_ORG=?, TARGET_SPACE=? WHERE ID=?";

    @Override
    public Map<Long, String> extractData(ResultSet resultSet) throws SQLException {
        Map<Long, String> result = new HashMap<>();
        while (resultSet.next()) {
            long id = resultSet.getLong("ID");
            String targetSpace = resultSet.getString("TARGET_SPACE");
            result.put(id, targetSpace);
            logger.debug(String.format("Retrieve data from row ID: '%s' and TARGET_SPACE: '%s'", id, targetSpace));
        }
        return result;
    }

    @Override
    public Map<Long, CloudTarget> transformData(Map<Long, String> retrievedData) {
        Map<Long, CloudTarget> result = new HashMap<>();
        for (Map.Entry<Long, String> originalEntry : retrievedData.entrySet()) {
            CloudTarget cloudTarget = ConfigurationEntriesUtil.splitTargetSpaceValue(originalEntry.getValue());
            result.put(originalEntry.getKey(), cloudTarget);
        }
        return result;
    }

    @Override
    public void setUpdateStatementParameters(PreparedStatement preparedStatement, Map<Long, CloudTarget> transformedData)
        throws SQLException {
        for (Map.Entry<Long, CloudTarget> entry : transformedData.entrySet()) {
            CloudTarget cloudTarget = entry.getValue();
            preparedStatement.setString(1, cloudTarget.getOrganizationName());
            preparedStatement.setString(2, cloudTarget.getSpaceName());
            preparedStatement.setLong(3, entry.getKey());
            preparedStatement.addBatch();
            logger.debug(String.format("Executed update for row ID: '%s' , TARGET_ORG: '%s' , TARGET_SPACE: '%s'", entry.getKey(),
                                       cloudTarget.getOrganizationName(), cloudTarget.getSpaceName()));
        }
    }

    @Override
    public String getSelectStatement() {
        return SELECT_STATEMENT;
    }

    @Override
    public String getUpdateStatement() {
        return UPDATE_STATEMENT;
    }

    @Override
    public String getConfirmationMessage() {
        return Messages.SPLIT_TARGET_SPACE_COLUMN;
    }

}
