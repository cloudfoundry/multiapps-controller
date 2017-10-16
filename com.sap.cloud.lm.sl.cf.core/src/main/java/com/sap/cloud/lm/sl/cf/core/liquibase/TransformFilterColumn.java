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

import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;

public class TransformFilterColumn extends AbstractDataTransformationChange {

    private static final String TABLE_NAME = "CONFIGURATION_SUBSCRIPTION";
    private static final String TARGET_SPACE = "targetSpace";
    private static final String SEARCH_QUERY = "Select ID, FILTER from CONFIGURATION_SUBSCRIPTION";
    private static final String UPDATE_QUERY = "UPDATE CONFIGURATION_SUBSCRIPTION SET FILTER=? WHERE ID=?";

    @Override
    public void customUpdate(PreparedStatement preparedStatement, Map.Entry<Long, String> entry) throws SQLException {
        preparedStatement.setString(1, entry.getValue());
        preparedStatement.setLong(2, entry.getKey());
        preparedStatement.addBatch();
        logger.debug(String.format("Executed update for row ID: '%s' , FILTER: '%s'", entry.getKey(), entry.getValue()));
    }

    @Override
    public Map<Long, String> customExtractData(ResultSet query) throws CustomChangeException, DatabaseException, SQLException {
        Map<Long, String> result = new HashMap<Long, String>();
        while (query.next()) {
            long id = query.getLong("ID");
            String filter = query.getString("FILTER");
            result.put(id, filter);
            logger.debug(String.format("Retrieve data from row ID: '%s' and FILTER: '%s'", id, filter));
        }
        return result;
    }

    @Override
    public Map<Long, String> transformData(Map<Long, String> retrievedData) {
        Map<Long, String> transformedData = new HashMap<Long, String>();
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
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public String getSearchQuery() {
        return SEARCH_QUERY;
    }

    @Override
    public String getUpdateQuery() {
        return UPDATE_QUERY;
    }

    @Override
    public String getConfirmationMessage() {
        return Messages.TRANSFORMED_FILTER_COLUMN;
    }
}
