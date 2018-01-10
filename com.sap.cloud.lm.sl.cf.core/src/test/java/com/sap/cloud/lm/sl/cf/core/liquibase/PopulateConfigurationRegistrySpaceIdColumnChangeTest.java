package com.sap.cloud.lm.sl.cf.core.liquibase;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.junit.Before;
import org.junit.Test;

import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;

public class PopulateConfigurationRegistrySpaceIdColumnChangeTest {

    private PopulateConfigurationRegistrySpaceIdColumnChange classUnderTest;
    private Map<CloudTarget, String> spaces;
    private UUID uuid = new UUID(1234l, 1234l);

    @Before
    public void setUp() {

        spaces = getSpaces();
        classUnderTest = new PopulateConfigurationRegistrySpaceIdColumnChange() {

            protected String getSpaceId(String org, String space, CloudFoundryClient cfClient) {
                return spaces.get(new CloudTarget(org, space));
            }
            
            protected CloudFoundryClient getCFClient() {
                return null;
            }
        };
    }

    @Test
    public void testTransformData() {

        Map<Long, String> transformedData = classUnderTest.transformData(getConfigurationEntries());

        assertEquals("Transformed data size must be 2 ", 2, transformedData.size());
        assertEquals("Transformed entry guid must be: " + uuid.toString() , uuid.toString(), transformedData.get(1l));
        assertEquals("Transformed entry guid must be: sap", "sap", transformedData.get(2l));
    }

    private Map<Long, CloudTarget> getConfigurationEntries() {

        Map<Long, CloudTarget> result = new HashMap<Long, CloudTarget>();
        result.put(1l, new CloudTarget("org1", "space1"));
        result.put(2l, new CloudTarget("", "sap"));
        result.put(3l, new CloudTarget("", ""));
        result.put(4l, new CloudTarget(null, null));
        result.put(5l, new CloudTarget("org", ""));

        return result;
    }

    private Map<CloudTarget, String> getSpaces() {

        Map<CloudTarget, String> result = new HashMap<>();
        result.put(new CloudTarget("org1", "space1"), uuid.toString());
        result.put(new CloudTarget("org2", "space2"), null);

        return result;
    }
}
