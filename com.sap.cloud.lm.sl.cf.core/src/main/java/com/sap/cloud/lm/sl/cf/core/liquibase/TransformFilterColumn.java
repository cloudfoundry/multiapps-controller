package com.sap.cloud.lm.sl.cf.core.liquibase;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationUtil;

import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;

public class TransformFilterColumn extends AbstractLiquibaseCustomChange {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransformFilterColumn.class);
    private static final String TABLE_NAME = "CONFIGURATION_SUBSCRIPTION";
    private static final String TARGET_SPACE = "targetSpace";
    private static final String SEARCH_QUERY = "Select ID, FILTER from CONFIGURATION_SUBSCRIPTION";
    private static final String UPDATE_QUERY = "UPDATE CONFIGURATION_SUBSCRIPTION SET FILTER=? WHERE ID=?";

    public void customUpdate(PreparedStatement preparedStatement, Map.Entry<Long, String> entry) throws SQLException {

        preparedStatement.setString(1, entry.getValue());
        preparedStatement.setLong(2, entry.getKey());
        preparedStatement.addBatch();
        getLogger().debug(String.format("Executed update for row ID: '%s' , FILTER: '%s'", entry.getKey(), entry.getValue()));
    }

    public Map<Long, String> customExtractData(ResultSet query) throws CustomChangeException, DatabaseException, SQLException {

        Map<Long, String> result = new HashMap<Long, String>();
        while (query.next()) {
            long id = query.getLong("ID");
            String filter = query.getString("FILTER");
            result.put(id, filter);
            LOGGER.debug(String.format("Retrieve data from row ID: '%s' and FILTER: '%s'", id, filter));
        }

        return result;
    }

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

            CloudTarget cloudTarget = ConfigurationUtil.splitTargetSpaceValue(targetSpaceJsonElement.getAsString());
            JsonElement cloudTargetJsonElement = new Gson().toJsonTree(cloudTarget);
            filterJsonObject.add(TARGET_SPACE, cloudTargetJsonElement);
            transformedData.put(entry.getKey(), filterJsonObject.toString());

            LOGGER.debug(String.format("Transform data for row ID: '%s' , Filter: '%s'", entry.getKey(), filterJsonObject.toString()));
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
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public String getUpdateQuery() {
        return UPDATE_QUERY;
    }
}
