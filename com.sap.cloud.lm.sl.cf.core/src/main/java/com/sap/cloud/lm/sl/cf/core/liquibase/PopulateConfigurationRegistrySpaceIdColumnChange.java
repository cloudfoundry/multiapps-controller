package com.sap.cloud.lm.sl.cf.core.liquibase;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudSpace;

import com.sap.cloud.lm.sl.cf.core.cf.clients.CFOptimizedSpaceGetter;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.util.Configuration;
import com.sap.cloud.lm.sl.cf.core.util.SecurityUtil;

public class PopulateConfigurationRegistrySpaceIdColumnChange
    extends AbstractDataTransformationChange<Map<Long, CloudTarget>, Map<Long, String>> {

    private static final String SELECT_STATEMENT = "SELECT ID, TARGET_SPACE, TARGET_ORG FROM CONFIGURATION_REGISTRY";
    private static final String UPDATE_STATEMENT = "UPDATE CONFIGURATION_REGISTRY SET SPACE_ID=? WHERE ID=?";
    private CFOptimizedSpaceGetter cfOptimizedSpaceGetter = new CFOptimizedSpaceGetter();

    @Override
    public Map<Long, String> transformData(Map<Long, CloudTarget> retrievedData) {
        HashMap<Long, String> result = new HashMap<>();
        CloudFoundryClient cfClient = getCFClient();
        String org, space;
        for (Long id : retrievedData.keySet()) {
            org = retrievedData.get(id).getOrg();
            space = retrievedData.get(id).getSpace();
            if (StringUtils.isEmpty(space)) {
                continue;
            }

            if (StringUtils.isEmpty(org)) {
                // This is required because in some rows there is no target organization and target space contains spaceId value
                result.put(id, space);
            } else {
                String spaceGuid = getSpaceId(org, space, cfClient);
                result.put(id, spaceGuid);
            }

        }
        return result;
    }

    protected String getSpaceId(String orgName, String spaceName, CloudFoundryClient cfClient) {
        
        CloudSpace cloudSpace = cfOptimizedSpaceGetter.findSpace(cfClient, orgName, spaceName);
        if(cloudSpace == null){
            return null;
        }
        
        return cloudSpace.getMeta().getGuid().toString();
    }

    @Override
    public Map<Long, CloudTarget> extractData(ResultSet resultSet) throws SQLException {

        Map<Long, CloudTarget> result = new HashMap<>();
        CloudTarget cloudTarget;
        while (resultSet.next()) {
            long id = resultSet.getLong("ID");
            String targetSpace = resultSet.getString("TARGET_SPACE");
            String targetOrg = resultSet.getString("TARGET_ORG");
            cloudTarget = new CloudTarget(targetOrg, targetSpace);
            result.put(id, cloudTarget);
            logger.debug(
                String.format("Retrieve data from row ID: '%s', TARGET_ORG: '%s' and TARGET_SPACE: '%s'", id, targetOrg, targetSpace));
        }

        return result;
    }

    @Override
    public void setUpdateStatementParameters(PreparedStatement preparedStatement, Map<Long, String> transformedData) throws SQLException {

        for (Map.Entry<Long, String> entry : transformedData.entrySet()) {
            String space_id = entry.getValue();
            preparedStatement.setString(1, space_id);
            preparedStatement.setLong(2, entry.getKey());
            preparedStatement.addBatch();
            logger.debug(String.format("Executed update for row ID: '%s' , SPACE_ID: '%s' ", entry.getKey(), space_id));
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
        return Messages.POPULATE_SPACE_ID_COLUMN;
    }

    protected CloudFoundryClient getCFClient() {

        CloudCredentials cloudCredentials = new CloudCredentials(Configuration.getInstance().getGlobalAuditorUser(),
            Configuration.getInstance().getGlobalAuditorPassword(), SecurityUtil.CLIENT_ID, SecurityUtil.CLIENT_SECRET);

        CloudFoundryClient cfClient = new CloudFoundryClient(cloudCredentials, Configuration.getInstance().getTargetURL(),
            Configuration.getInstance().shouldSkipSslValidation());
        cfClient.login();

        return cfClient;
    }
}
