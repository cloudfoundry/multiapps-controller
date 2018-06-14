package com.sap.cloud.lm.sl.cf.core.helpers.v2_0;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.dao.filters.ConfigurationFilter;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ResolvedConfigurationReference;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.mta.handlers.v2_0.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.handlers.v2_0.DescriptorParser;
import com.sap.cloud.lm.sl.mta.model.v2_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2_0.Resource;

@RunWith(Parameterized.class)
public class ConfigurationSubscriptionFactoryTest {

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) The required dependency is managed, so a subscription should be created:
            {
                "subscriptions-mtad-00.yaml", Arrays.asList("plugins"), "SPACE_ID_1", "R:subscriptions-00.json",
            },
            // (1) The required dependency is not managed, so a subscription should not be created:
            {
                "subscriptions-mtad-01.yaml", Arrays.asList("plugins"), "SPACE_ID_1", "[]",
            },
            // (2) The required dependency is not managed, so a subscription should not be created:
            {
                "subscriptions-mtad-02.yaml", Arrays.asList("plugins"), "SPACE_ID_1", "[]",
            },
// @formatter:on
        });
    }

    private String mtadFilePath;
    private List<String> configurationResources;
    private String spaceId;
    private String expected;

    public ConfigurationSubscriptionFactoryTest(String mtadFilePath, List<String> configurationResources, String spaceId, String expected) {
        this.mtadFilePath = mtadFilePath;
        this.configurationResources = configurationResources;
        this.spaceId = spaceId;
        this.expected = expected;
    }

    @Test
    public void testCreate() throws Exception {
        String mtadString = TestUtil.getResourceAsString(mtadFilePath, getClass());
        DeploymentDescriptor mtad = getDescriptorParser().parseDeploymentDescriptorYaml(mtadString);
        Map<String, ResolvedConfigurationReference> resolvedResources = getResolvedConfigurationReferences(mtad);
        testCreate(mtad, resolvedResources, spaceId, expected);
    }

    protected void testCreate(DeploymentDescriptor mtad, Map<String, ResolvedConfigurationReference> resolvedResources, String spaceId, String expected) {
        TestUtil.test(() -> {
            return new ConfigurationSubscriptionFactory().create(mtad, resolvedResources, spaceId);
        }, expected, getClass());
    }

    private Map<String, ResolvedConfigurationReference> getResolvedConfigurationReferences(DeploymentDescriptor descriptor) {
        Map<String, ResolvedConfigurationReference> result = new HashMap<>();
        for (String configurationResource : configurationResources) {
            result.put(configurationResource, getResolvedConfigurationReference(descriptor, configurationResource));
        }
        return result;
    }

    private ResolvedConfigurationReference getResolvedConfigurationReference(DeploymentDescriptor descriptor,
        String configurationResource) {
        DescriptorHandler handler = new DescriptorHandler();
        Resource resource = (Resource) handler.findResource(descriptor, configurationResource);
        return new ResolvedConfigurationReference(createDummyFilter(), resource, Collections.emptyList());
    }

    private ConfigurationFilter createDummyFilter() {
        return new ConfigurationFilter("mta", "com.sap.other.mta", "1.0.0", new CloudTarget("ORG", "SPACE"), Collections.emptyMap());
    }

    protected DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }

}
