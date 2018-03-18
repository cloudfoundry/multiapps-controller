package com.sap.cloud.lm.sl.cf.core.persistence.changes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;
import com.sap.cloud.lm.sl.cf.core.cf.clients.CFOptimizedSpaceGetter;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.util.Configuration;
import com.sap.cloud.lm.sl.cf.core.util.SecurityUtil;
import com.sap.cloud.lm.sl.persistence.changes.AsyncChange;

@Component
public class PopulateConfigurationRegistrySpaceIdColumnChange implements AsyncChange {

    private static final Logger LOGGER = LoggerFactory.getLogger(PopulateConfigurationRegistrySpaceIdColumnChange.class);
    private CFOptimizedSpaceGetter cfOptimizedSpaceGetter = new CFOptimizedSpaceGetter();

    @Inject
    private ConfigurationEntryDao entryDao;

    @Inject
    private Configuration configuration;

    @Override
    public void execute(DataSource dataSource) {
        PlatformType platformType = configuration.getPlatformType();
        if (PlatformType.CF != platformType) {
            return;
        }
        LOGGER.info("Executing PopulateConfigurationRegistrySpaceIdColumnChange...");
        Map<Long, ConfigurationEntry> extractedConfigurationEntries = extractData();
        Map<Long, ConfigurationEntry> transformedConfigurationEntries = transformData(extractedConfigurationEntries);
        updateConfigurationEntries(transformedConfigurationEntries);
        LOGGER.info("Executed PopulateConfigurationRegistrySpaceIdColumnChange.");
    }

    private void updateConfigurationEntries(Map<Long, ConfigurationEntry> transformedData) {
        for (Long id : transformedData.keySet()) {
            ConfigurationEntry configurationEntry = transformedData.get(id);
            entryDao.update(id, configurationEntry);
        }
    }

    protected Map<Long, ConfigurationEntry> transformData(Map<Long, ConfigurationEntry> retrievedData) {
        HashMap<Long, ConfigurationEntry> result = new HashMap<>();
        CloudFoundryOperations cfClient = getCFClient();
        for (Long id : retrievedData.keySet()) {
            CloudTarget cloudTarget = retrievedData.get(id)
                .getTargetSpace();
            String organization = cloudTarget.getOrg();
            String space = cloudTarget.getSpace();
            if (StringUtils.isEmpty(space)) {
                continue;
            }
            ConfigurationEntry configurationEntry = retrievedData.get(id);
            if (StringUtils.isEmpty(organization)) {
                // This is required because in some rows there is no target organization and target space contains spaceId value
                configurationEntry.setSpaceId(space);
                result.put(id, configurationEntry);
            } else {
                String spaceGuid = getSpaceId(organization, space, cfClient);
                configurationEntry.setSpaceId(spaceGuid);
                result.put(id, configurationEntry);
            }
        }
        return result;
    }

    protected Map<Long, ConfigurationEntry> extractData() {
        Map<Long, ConfigurationEntry> result = new HashMap<>();
        List<ConfigurationEntry> allConfigurationEntries = getAllConfigurationEntries();

        for (ConfigurationEntry configurationEntry : allConfigurationEntries) {
            if (StringUtils.isNotEmpty(configurationEntry.getSpaceId())) {
                continue;
            }
            long id = configurationEntry.getId();
            CloudTarget cloudTarget = configurationEntry.getTargetSpace();
            result.put(id, configurationEntry);
            LOGGER.debug(String.format("Retrieve data from row ID: '%s', TARGET_ORG: '%s' and TARGET_SPACE: '%s'", id, cloudTarget.getOrg(),
                cloudTarget.getSpace()));
        }
        return result;
    }

    protected List<ConfigurationEntry> getAllConfigurationEntries() {
        return entryDao.findAll();
    }

    protected String getSpaceId(String orgName, String spaceName, CloudFoundryOperations cfClient) {
        CloudSpace cloudSpace = cfOptimizedSpaceGetter.findSpace(cfClient, orgName, spaceName);
        if (cloudSpace == null) {
            return null;
        }

        return cloudSpace.getMeta()
            .getGuid()
            .toString();
    }

    protected CloudFoundryOperations getCFClient() {
        CloudCredentials cloudCredentials = new CloudCredentials(Configuration.getInstance()
            .getGlobalAuditorUser(),
            Configuration.getInstance()
                .getGlobalAuditorPassword(),
            SecurityUtil.CLIENT_ID, SecurityUtil.CLIENT_SECRET);

        CloudFoundryOperations cfClient = new CloudFoundryClient(cloudCredentials, configuration.getTargetURL(),
            configuration.shouldSkipSslValidation());
        cfClient.login();

        return cfClient;
    }
}
