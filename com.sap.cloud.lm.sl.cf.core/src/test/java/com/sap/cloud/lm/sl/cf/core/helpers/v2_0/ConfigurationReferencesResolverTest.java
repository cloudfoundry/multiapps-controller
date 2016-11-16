package com.sap.cloud.lm.sl.cf.core.helpers.v2_0;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.mta.builders.v2_0.ParametersChainBuilder;
import com.sap.cloud.lm.sl.mta.handlers.v2_0.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.handlers.v2_0.DescriptorParser;
import com.sap.cloud.lm.sl.mta.model.v2_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2_0.TargetPlatform;
import com.sap.cloud.lm.sl.mta.model.v2_0.TargetPlatformType;

@RunWith(Parameterized.class)
public class ConfigurationReferencesResolverTest extends com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ConfigurationReferencesResolverTest {

    private static TargetPlatformType platformType;
    private static TargetPlatform platform;

    public ConfigurationReferencesResolverTest(String descriptorLocation, String configurationEntriesLocation, String expectedDescriptor)
        throws Exception {
        super(descriptorLocation, configurationEntriesLocation, expectedDescriptor);
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Reference to existing provided dependency:
            {
                "mtad-03.yaml", "configuration-entries-01.json", "R:result-01.json",
            },
            // (1) Use new syntax:
            {
                "mtad-05.yaml", "configuration-entries-01.json", "R:result-01.json",
            },
            // (2) Use new syntax when more than one configuration entries are available:
            {
                "mtad-05.yaml", "configuration-entries-05.json", "E:Multiple configuration entries were found matching the filter specified in resource \"resource-2\"",
            },
            // (3) Use new syntax when more than one configuration entries are available:
            {
                "mtad-07.yaml", "configuration-entries-06.json", "R:result-02.json",
            },
            // (4) Use new syntax when there is no configuration entry available:
            {
                "mtad-05.yaml", "configuration-entries-04.json", "E:No configuration entries were found matching the filter specified in resource \"resource-2\"",
            },
            // (5) Use new syntax when there is no configuration entry available:
            {
                "mtad-07.yaml", "configuration-entries-07.json", "R:result-03.json",
            },
            // (6) Use new syntax (missing org parameter):
            {
                "mtad-06.yaml", "configuration-entries-01.json", "E:Could not find required property \"org\"",
            },
            // (7) Subscriptions should be created:
            {
                "mtad-08.yaml", "configuration-entries-06.json", "R:result-04.json",
            },
// @formatter:on
        });
    }

    @BeforeClass
    public static void initializePlatformAndPlatformType() throws Exception {
        ConfigurationParser parser = new ConfigurationParser();
        platform = parser.parsePlatformsJson2_0(
            ConfigurationReferencesResolverTest.class.getResourceAsStream("/mta/platforms-v2.json")).get(2);
        platformType = parser.parsePlatformTypesJson2_0(
            ConfigurationReferencesResolverTest.class.getResourceAsStream("/mta/platform-types-v2.json")).get(0);
    }

    @Override
    protected ParametersChainBuilder getPropertiesChainBuilder(com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor descriptor) {
        return new ParametersChainBuilder((DeploymentDescriptor) descriptor, platform, platformType);
    }

    @Override
    protected DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }

    @Override
    protected ConfigurationReferencesResolver getConfigurationResolver(
        com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor deploymentDescriptor) {
        return new ConfigurationReferencesResolver(dao,
            new ConfigurationFilterParser(platformType, platform, getPropertiesChainBuilder(descriptor)), (org, space) -> SPACE_ID);
    }

}
