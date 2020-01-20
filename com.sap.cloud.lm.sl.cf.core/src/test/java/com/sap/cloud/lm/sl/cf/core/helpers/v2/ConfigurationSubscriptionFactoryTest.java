package com.sap.cloud.lm.sl.cf.core.helpers.v2;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationFilter;
import com.sap.cloud.lm.sl.cf.core.model.ResolvedConfigurationReference;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.Tester;
import com.sap.cloud.lm.sl.common.util.Tester.Expectation;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorParser;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Resource;

@RunWith(Parameterized.class)
public class ConfigurationSubscriptionFactoryTest {

    protected final Tester tester = Tester.forClass(getClass());

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) The required dependency is managed, so a subscription should be created:
            {
                "subscriptions-mtad-00.yaml", Collections.singletonList("plugins"), "SPACE_ID_1", new Expectation(Expectation.Type.JSON, "subscriptions-00.json"),
            },
            // (1) The required dependency is not managed, so a subscription should not be created:
            {
                "subscriptions-mtad-01.yaml", Collections.singletonList("plugins"), "SPACE_ID_1", new Expectation("[]"),
            },
            // (2) The required dependency is not managed, so a subscription should not be created:
            {
                "subscriptions-mtad-02.yaml", Collections.singletonList("plugins"), "SPACE_ID_1", new Expectation("[]"),
            },
// @formatter:on
        });
    }

    private final String mtadFilePath;
    private final List<String> configurationResources;
    private final String spaceId;
    private final Expectation expectation;

    public ConfigurationSubscriptionFactoryTest(String mtadFilePath, List<String> configurationResources, String spaceId,
                                                Expectation expectation) {
        this.mtadFilePath = mtadFilePath;
        this.configurationResources = configurationResources;
        this.spaceId = spaceId;
        this.expectation = expectation;
    }

    @Test
    public void testCreate() {
        String mtadString = TestUtil.getResourceAsString(mtadFilePath, getClass());
        DeploymentDescriptor mtad = getDescriptorParser().parseDeploymentDescriptorYaml(mtadString);
        Map<String, ResolvedConfigurationReference> resolvedResources = getResolvedConfigurationReferences(mtad);
        testCreate(mtad, resolvedResources, spaceId, expectation);
    }

    protected void testCreate(DeploymentDescriptor mtad, Map<String, ResolvedConfigurationReference> resolvedResources, String spaceId,
                              Expectation expectation) {
        tester.test(() -> new ConfigurationSubscriptionFactory(mtad, resolvedResources).create(spaceId), expectation);
    }

    private Map<String, ResolvedConfigurationReference> getResolvedConfigurationReferences(DeploymentDescriptor descriptor) {
        return configurationResources.stream()
                                     .collect(Collectors.toMap(resource -> resource,
                                                               resource -> getResolvedConfigurationReference(descriptor, resource)));
    }

    private ResolvedConfigurationReference getResolvedConfigurationReference(DeploymentDescriptor descriptor,
                                                                             String configurationResource) {
        DescriptorHandler handler = new DescriptorHandler();
        Resource resource = handler.findResource(descriptor, configurationResource);
        return new ResolvedConfigurationReference(createDummyFilter(), resource, Collections.emptyList());
    }

    private ConfigurationFilter createDummyFilter() {
        return new ConfigurationFilter("mta", "com.sap.other.mta", "1.0.0", new CloudTarget("ORG", "SPACE"), Collections.emptyMap());
    }

    protected DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }

}
