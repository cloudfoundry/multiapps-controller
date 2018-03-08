package com.sap.cloud.lm.sl.cf.process.jobs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;
import com.sap.cloud.lm.sl.cf.core.cf.clients.CFOptimizedSpaceGetter;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.util.Configuration;
import com.sap.cloud.lm.sl.cf.core.util.SecurityUtil;

@Service
public class PopulateConfigurationRegistrySpaceIdColumnJob implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PopulateConfigurationRegistrySpaceIdColumnJob.class);
    private CFOptimizedSpaceGetter cfOptimizedSpaceGetter = new CFOptimizedSpaceGetter();

    @Inject
    private ConfigurationEntryDao entryDao;

    @Inject
    private Configuration configuration;

    @Override
    public void run() {
        PlatformType platformType = Configuration.getInstance().getPlatformType();
        if (PlatformType.CF != platformType) {
            return;
        }
        LOGGER.info("Start executing PopulateConfigurationRegistrySpaceIdColumn job...");
        Map<Long, ConfigurationEntry> extractedConfigurationEntries = extractData();
        Map<Long, ConfigurationEntry> transformedonfigurationEntries = transformData(extractedConfigurationEntries);
        updateConfigurationEntries(transformedonfigurationEntries);
        LOGGER.info("Finish executing PopulateConfigurationRegistrySpaceIdColumn job.");
    }

    private void updateConfigurationEntries(Map<Long, ConfigurationEntry> transformedData) {
        ConfigurationEntry configurationEntry;
        for (Long id : transformedData.keySet()) {
            configurationEntry = transformedData.get(id);
            entryDao.update(id, configurationEntry);
        }
    }

    protected Map<Long, ConfigurationEntry> transformData(Map<Long, ConfigurationEntry> retrievedData) {
        HashMap<Long, ConfigurationEntry> result = new HashMap<>();
        CloudFoundryClient cfClient = getCFClient();
        CloudTarget cloudTarget;
        String organization, space;
        for (Long id : retrievedData.keySet()) {
            cloudTarget = retrievedData.get(id)
                .getTargetSpace();
            organization = cloudTarget.getOrg();
            space = cloudTarget.getSpace();
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
        CloudTarget cloudTarget;
        List<ConfigurationEntry> allConfigurationEntries = getAllConfigurationEntries();

        for (ConfigurationEntry configurationEntry : allConfigurationEntries) {
            if (StringUtils.isNotEmpty(configurationEntry.getSpaceId())) {
                continue;
            }
            long id = configurationEntry.getId();
            cloudTarget = configurationEntry.getTargetSpace();
            result.put(id, configurationEntry);
            LOGGER.debug(String.format("Retrieve data from row ID: '%s', TARGET_ORG: '%s' and TARGET_SPACE: '%s'", id, cloudTarget.getOrg(),
                cloudTarget.getSpace()));
        }
        return result;
    }

    protected List<ConfigurationEntry> getAllConfigurationEntries() {
        return entryDao.findAll();
    }

    protected String getSpaceId(String orgName, String spaceName, CloudFoundryClient cfClient) {
        CloudSpace cloudSpace = cfOptimizedSpaceGetter.findSpace(cfClient, orgName, spaceName);
        if (cloudSpace == null) {
            return null;
        }

        return cloudSpace.getMeta()
            .getGuid()
            .toString();
    }

    protected CloudFoundryClient getCFClient() {
        CloudCredentials cloudCredentials = new CloudCredentials(Configuration.getInstance()
            .getGlobalAuditorUser(),
            Configuration.getInstance()
                .getGlobalAuditorPassword(),
            SecurityUtil.CLIENT_ID, SecurityUtil.CLIENT_SECRET);

        CloudFoundryClient cfClient = new CloudFoundryClient(cloudCredentials, configuration.getTargetURL(),
            configuration.shouldSkipSslValidation());
        cfClient.login();

        return cfClient;
    }
}
