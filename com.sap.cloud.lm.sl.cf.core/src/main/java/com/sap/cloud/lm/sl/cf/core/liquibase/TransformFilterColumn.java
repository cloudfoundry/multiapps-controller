package com.sap.cloud.lm.sl.cf.core.liquibase;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil;
import com.sap.cloud.lm.sl.persistence.liquibase.AbstractDataTransformationChange;

public class TransformFilterColumn extends AbstractDataTransformationChange<Map<Long, String>, Map<Long, String>> {

    private static final String TARGET_SPACE = "targetSpace";
    private static final String SELECT_STATEMENT = "SELECT ID, FILTER FROM CONFIGURATION_SUBSCRIPTION";
    private static final String UPDATE_STATEMENT = "UPDATE CONFIGURATION_SUBSCRIPTION SET FILTER=? WHERE ID=?";

    @Override
    public Map<Long, String> extractData(ResultSet resultSet) throws SQLException {
        Map<Long, String> result = new HashMap<>();
        while (resultSet.next()) {
            long id = resultSet.getLong("ID");
            String filter = resultSet.getString("FILTER");
            result.put(id, filter);
            logger.debug(String.format("Retrieve data from row ID: '%s' and FILTER: '%s'", id, filter));
        }
        return result;
    }

    @Override
    public Map<Long, String> transformData(Map<Long, String> retrievedData) {
        Map<Long, String> transformedData = new HashMap<>();
        for (Map.Entry<Long, String> entry : retrievedData.entrySet()) {

            if (StringUtils.isEmpty(entry.getValue())) {
                continue;
            }
            JsonElement initialFilterJsonElement = new JsonParser().parse(entry.getValue());
            JsonObject filterJsonObject = initialFilterJsonElement.getAsJsonObject();
            JsonElement targetSpaceJsonElement = filterJsonObject.get(TARGET_SPACE);
            if (targetSpaceJsonElement == null) {
                continue;
            }

            CloudTarget cloudTarget = ConfigurationEntriesUtil.splitTargetSpaceValue(targetSpaceJsonElement.getAsString());
            JsonElement cloudTargetJsonElement = new Gson().toJsonTree(cloudTarget);
            filterJsonObject.add(TARGET_SPACE, cloudTargetJsonElement);
            transformedData.put(entry.getKey(), filterJsonObject.toString());

            logger.debug(String.format("Transform data for row ID: '%s' , Filter: '%s'", entry.getKey(), filterJsonObject.toString()));
        }
        return transformedData;
    }

    @Override
    public void setUpdateStatementParameters(PreparedStatement preparedStatement, Map<Long, String> transformedData) throws SQLException {
        for (Map.Entry<Long, String> entry : transformedData.entrySet()) {
            preparedStatement.setString(1, entry.getValue());
            preparedStatement.setLong(2, entry.getKey());
            preparedStatement.addBatch();
            logger.debug(String.format("Executed update for row ID: '%s' , FILTER: '%s'", entry.getKey(), entry.getValue()));
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
        return Messages.TRANSFORMED_FILTER_COLUMN;
    }

}
