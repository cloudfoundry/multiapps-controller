package com.sap.cloud.lm.sl.cf.core.helpers.v1;

import static com.sap.cloud.lm.sl.common.util.TestUtil.getResourceAsString;
import static org.mockito.Mockito.when;

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
import com.sap.cloud.lm.sl.cf.core.dao.filters.ConfigurationFilter;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;
import com.sap.cloud.lm.sl.mta.builders.v2.PropertiesChainBuilder;
import com.sap.cloud.lm.sl.mta.handlers.v2.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorParser;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.Platform;

@RunWith(Parameterized.class)
public class ConfigurationReferencesResolverTest {

    protected static final String SPACE_ID = "SAP";

    protected static class DaoMockConfiguration {

        ConfigurationFilter filter;
        List<ConfigurationEntry> configurationEntries;

    }

    private static Platform platform;

    private String descriptorLocation;
    private Expectation expectation;

    protected ConfigurationEntryDao dao = Mockito.mock(ConfigurationEntryDao.class);
    protected List<DaoMockConfiguration> daoConfigurations;
    protected DeploymentDescriptor descriptor;
    protected ApplicationConfiguration configuration = Mockito.mock(ApplicationConfiguration.class);

    public ConfigurationReferencesResolverTest(String descriptorLocation, String daoConfigurationsLocation, Expectation expectation)
        throws Exception {
        this.daoConfigurations = JsonUtil.fromJson(getResourceAsString(daoConfigurationsLocation, getClass()),
            new TypeToken<List<DaoMockConfiguration>>() {
            }.getType());
        this.descriptorLocation = descriptorLocation;
        this.expectation = expectation;
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Reference to existing provided dependency in the same space:
            {
                "mtad-03.yaml", "configuration-entries-01.json", new Expectation(Expectation.Type.RESOURCE, "result.json"),
            },
            // (1) Reference with some missing parameters:
            {
                "mtad-04.yaml", "configuration-entries-01.json", new Expectation(Expectation.Type.EXCEPTION, "Could not find required property \"mta-version\"")
            },
            // (2) Multiple configuration entries exist matching the filter:
            {
                "mtad-05.yaml", "configuration-entries-02.json", new Expectation(Expectation.Type.EXCEPTION, "Multiple configuration entries were found matching the filter specified in resource \"resource-2\"")
            },
            // (3) No configuration entries matching the filter:
            {
                "mtad-06.yaml", "configuration-entries-03.json", new Expectation(Expectation.Type.EXCEPTION, "No configuration entries were found matching the filter specified in resource \"resource-2\"")
            }
// @formatter:on
        });
    }

    @BeforeClass
    public static void initializePlatformAndPlatformType() throws Exception {
        ConfigurationParser parser = new ConfigurationParser();
        platform = parser.parsePlatformJson(ConfigurationReferencesResolverTest.class.getResourceAsStream("/mta/xs-platform.json"));
    }

    @Before
    public void setUp() throws Exception {
        this.descriptor = getDescriptorParser().parseDeploymentDescriptorYaml(getClass().getResourceAsStream(descriptorLocation));

        for (DaoMockConfiguration configuration : daoConfigurations) {
            ConfigurationFilter filter = configuration.filter;
            when(dao.find(filter.getProviderNid(), filter.getProviderId(), filter.getProviderVersion(), filter.getTargetSpace(),
                filter.getRequiredContent(), null, null)).thenReturn(configuration.configurationEntries);
        }
    }

    @Test
    public void testResolve() {
        ConfigurationReferencesResolver referencesResolver = getConfigurationResolver(descriptor);

        TestUtil.test(() -> {

            referencesResolver.resolve(descriptor);
            return descriptor;

        }, expectation, getClass());
    }

    protected ConfigurationReferencesResolver getConfigurationResolver(DeploymentDescriptor descriptor) {
        String currentOrg = (String) platform.getProperties()
            .get("org");
        String currentSpace = (String) platform.getProperties()
            .get("space");
        return new ConfigurationReferencesResolver(dao,
            new ConfigurationFilterParser(new CloudTarget(currentOrg, currentSpace), getPropertiesChainBuilder(descriptor)),
            (org, space) -> SPACE_ID, null, configuration);
    }

    protected DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }

    protected PropertiesChainBuilder getPropertiesChainBuilder(DeploymentDescriptor descriptor) {
        return new PropertiesChainBuilder(descriptor, platform);
    }

}
