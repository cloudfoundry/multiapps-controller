package com.sap.cloud.lm.sl.cf.core.helpers.v1_0;

import static com.sap.cloud.lm.sl.common.util.TestUtil.getResourceAsString;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import com.google.gson.reflect.TypeToken;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationFilter;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.mta.builders.v1_0.PropertiesChainBuilder;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorParser;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatformType;

@RunWith(Parameterized.class)
public class ConfigurationReferencesResolverTest {

    protected static final String SPACE_ID = "SAP";

    protected static class DaoMockConfiguration {

        ConfigurationFilter filter;
        List<ConfigurationEntry> configurationEntries;

    }

    private static TargetPlatformType platformType;
    private static TargetPlatform platform;

    private String descriptorLocation;
    private String expectedDescriptor;

    protected ConfigurationEntryDao dao = Mockito.mock(ConfigurationEntryDao.class);
    protected List<DaoMockConfiguration> daoConfigurations;
    protected DeploymentDescriptor descriptor;

    public ConfigurationReferencesResolverTest(String descriptorLocation, String daoConfigurationsLocation, String expectedDescriptor)
        throws Exception {
        this.daoConfigurations = JsonUtil.fromJson(getResourceAsString(daoConfigurationsLocation, getClass()),
            new TypeToken<List<DaoMockConfiguration>>() {
            }.getType());
        this.descriptorLocation = descriptorLocation;
        this.expectedDescriptor = expectedDescriptor;
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Reference to existing provided dependency in the same space:
            {
                "mtad-03.yaml", "configuration-entries-01.json", "R:result.json",
            },
            // (1) Reference with some missing parameters:
            {
                "mtad-04.yaml", "configuration-entries-01.json", "E:Could not find required property \"mta-version\"",
            },
            // (2) Multiple configuration entries exist matching the filter:
            {
                "mtad-05.yaml", "configuration-entries-02.json", "E:Multiple configuration entries were found matching the filter specified in resource \"resource-2\"",
            },
            // (3) No configuration entries matching the filter:
            {
                "mtad-06.yaml", "configuration-entries-03.json", "E:No configuration entries were found matching the filter specified in resource \"resource-2\"",
            },
// @formatter:on
        });
    }

    @BeforeClass
    public static void initializePlatformAndPlatformType() throws Exception {
        ConfigurationParser parser = new ConfigurationParser();
        platform = parser.parsePlatformsJson(ConfigurationReferencesResolverTest.class.getResourceAsStream("/mta/platforms.json")).get(2);
        platformType = parser.parsePlatformTypesJson(
            ConfigurationReferencesResolverTest.class.getResourceAsStream("/mta/platform-types.json")).get(0);
    }

    @Before
    public void setUp() throws Exception {
        this.descriptor = getDescriptorParser().parseDeploymentDescriptorYaml(getClass().getResourceAsStream(descriptorLocation));

        for (DaoMockConfiguration configuration : daoConfigurations) {
            ConfigurationFilter filter = configuration.filter;
            Mockito.when(dao.find(filter.getProviderNid(), filter.getProviderId(), filter.getProviderVersion(), filter.getTargetSpace(),
                filter.getRequiredContent(), null)).thenReturn(configuration.configurationEntries);
        }
    }

    @Test
    public void testResolve() {
        ConfigurationReferencesResolver referencesResolver = getConfigurationResolver(descriptor);

        TestUtil.test(() -> {

            referencesResolver.resolve(descriptor);
            return descriptor;

        } , expectedDescriptor, getClass());
    }

    protected ConfigurationReferencesResolver getConfigurationResolver(DeploymentDescriptor descriptor) {
        return new ConfigurationReferencesResolver(dao,
            new ConfigurationFilterParser(platformType, platform, getPropertiesChainBuilder(descriptor)), (org, space) -> SPACE_ID);
    }

    protected DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }

    protected PropertiesChainBuilder getPropertiesChainBuilder(DeploymentDescriptor descriptor) {
        return new PropertiesChainBuilder(descriptor, platform, platformType);
    }

}
