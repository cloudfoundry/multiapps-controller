package com.sap.cloud.lm.sl.cf.core.helpers.v3_1;

import java.util.Arrays;
import java.util.List;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.mta.handlers.v3_1.DescriptorParser;

@RunWith(Parameterized.class)
public class ConfigurationSubscriptionFactoryTest extends com.sap.cloud.lm.sl.cf.core.helpers.v2_0.ConfigurationSubscriptionFactoryTest {

    public ConfigurationSubscriptionFactoryTest(String mtadFilePath, List<String> configurationResources, String spaceId, String expected) {
        super(mtadFilePath, configurationResources, spaceId, expected);
    }

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

    @Override
    protected ConfigurationSubscriptionFactory getConfigurationSubscriptionFactory() {
        return new ConfigurationSubscriptionFactory();
    }

    @Override
    protected DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }

}
