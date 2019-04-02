package com.sap.cloud.lm.sl.cf.core.helpers.v3;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.model.ResolvedConfigurationReference;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;
import com.sap.cloud.lm.sl.mta.handlers.v3.DescriptorParser;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;

@RunWith(Parameterized.class)
public class ConfigurationSubscriptionFactoryTest extends com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationSubscriptionFactoryTest {

    public ConfigurationSubscriptionFactoryTest(String mtadFilePath, List<String> configurationResources, String spaceId,
        Expectation expectation) {
        super(mtadFilePath, configurationResources, spaceId, expectation);
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) The required dependency is managed, so a subscription should be created:
            {
                "subscriptions-mtad-00.yaml", Arrays.asList("plugins"), "SPACE_ID_1", new Expectation(Expectation.Type.RESOURCE, "subscriptions-00.json"),
            },
            // (1) The required dependency is not managed, so a subscription should not be created:
            {
                "subscriptions-mtad-01.yaml", Arrays.asList("plugins"), "SPACE_ID_1", new Expectation("[]"),
            },
            // (2) The required dependency is not managed, so a subscription should not be created:
            {
                "subscriptions-mtad-02.yaml", Arrays.asList("plugins"), "SPACE_ID_1", new Expectation("[]"),
            },
            // (3) The required dependency is not active, so a subscription should not be created:
            {
                "subscriptions-mtad-03.yaml", Arrays.asList("plugins"), "SPACE_ID_1", new Expectation("[]"),
            }
// @formatter:on
        });
    }

    @Override
    protected void testCreate(DeploymentDescriptor mtad, Map<String, ResolvedConfigurationReference> resolvedResources, String spaceId,
        Expectation expectation) {
        TestUtil.test(() -> {
            return new ConfigurationSubscriptionFactory().create(mtad, resolvedResources, spaceId);
        }, expectation, getClass());
    }

    @Override
    protected DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }

}
