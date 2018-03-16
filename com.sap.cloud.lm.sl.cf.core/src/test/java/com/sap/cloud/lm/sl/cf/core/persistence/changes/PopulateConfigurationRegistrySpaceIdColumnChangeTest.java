package com.sap.cloud.lm.sl.cf.core.persistence.changes;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.junit.Before;
import org.junit.Test;

import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;

public class PopulateConfigurationRegistrySpaceIdColumnChangeTest {

    private PopulateConfigurationRegistrySpaceIdColumnChange change;
    private Map<CloudTarget, String> spaces;
    private UUID uuid = new UUID(1234l, 1234l);

    @Before
    public void setUp() {

        spaces = getSpaces();
        change = new PopulateConfigurationRegistrySpaceIdColumnChange() {

            protected String getSpaceId(String org, String space, CloudFoundryClient cfClient) {
                return spaces.get(new CloudTarget(org, space));
            }

            protected CloudFoundryClient getCFClient() {
                return null;
            }

            protected List<ConfigurationEntry> getAllConfigurationEntries() {
                List<ConfigurationEntry> configurationEntries = new ArrayList<ConfigurationEntry>(getConfigurationEntries().values());
                configurationEntries.add(new ConfigurationEntry(6, null, null, null, new CloudTarget("org", "sap"), null, null, "spaceId"));
                configurationEntries
                    .add(new ConfigurationEntry(7, null, null, null, new CloudTarget("testorg", "testsap"), null, null, "testspaceId"));
                return configurationEntries;
            }
        };
    }

    @Test
    public void testTransformData() {
        Map<Long, ConfigurationEntry> transformedData = change.transformData(getConfigurationEntries());

        assertEquals("Transformed data size must be 2 ", 2, transformedData.size());
        assertEquals("Transformed entry guid must be: " + uuid.toString(), uuid.toString(), transformedData.get(1l)
            .getSpaceId());
        assertEquals("Transformed entry guid must be: sap", "sap", transformedData.get(2l)
            .getSpaceId());
    }

    @Test
    public void testFilterAlreadyPopulatedSpaceIds() {
        Map<Long, ConfigurationEntry> extractedData = change.extractData();
        assertEquals("Transformed data size must be 5 ", 5, extractedData.size());
    }

    private Map<Long, ConfigurationEntry> getConfigurationEntries() {
        Map<Long, ConfigurationEntry> result = new HashMap<>();
        result.put(1l, new ConfigurationEntry(1, null, null, null, new CloudTarget("org1", "space1"), null, null, null));
        result.put(2l, new ConfigurationEntry(2, null, null, null, new CloudTarget("", "sap"), null, null, null));
        result.put(3l, new ConfigurationEntry(3, null, null, null, new CloudTarget("", ""), null, null, null));
        result.put(4l, new ConfigurationEntry(4, null, null, null, new CloudTarget(null, null), null, null, null));
        result.put(5l, new ConfigurationEntry(5, null, null, null, new CloudTarget("org", ""), null, null, null));

        return result;
    }

    private Map<CloudTarget, String> getSpaces() {
        Map<CloudTarget, String> result = new HashMap<>();
        result.put(new CloudTarget("org1", "space1"), uuid.toString());
        result.put(new CloudTarget("org2", "space2"), null);

        return result;
    }
}
