package com.sap.cloud.lm.sl.cf.core.helpers.v2;

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
import com.sap.cloud.lm.sl.mta.builders.v2.ParametersChainBuilder;
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

    public ConfigurationReferencesResolverTest(String descriptorLocation, String configurationEntriesLocation, Expectation expectation)
        throws Exception {
        this.daoConfigurations = JsonUtil.fromJson(getResourceAsString(configurationEntriesLocation, getClass()),
            new TypeToken<List<DaoMockConfiguration>>() {
            }.getType());
        this.descriptorLocation = descriptorLocation;
        this.expectation = expectation;
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Reference to existing provided dependency:
            {
                "mtad-03.yaml", "configuration-entries-01.json", new Expectation(Expectation.Type.RESOURCE, "result-01.json"),
            },
            // (1) Use new syntax:
            {
                "mtad-05.yaml", "configuration-entries-01.json", new Expectation(Expectation.Type.RESOURCE, "result-01.json"),
            },
            // (2) Use new syntax when more than one configuration entries are available:
            {
                "mtad-05.yaml", "configuration-entries-05.json", new Expectation(Expectation.Type.EXCEPTION, "Multiple configuration entries were found matching the filter specified in resource \"resource-2\"")
            },
            // (3) Use new syntax when more than one configuration entries are available:
            {
                "mtad-07.yaml", "configuration-entries-06.json", new Expectation(Expectation.Type.RESOURCE, "result-02.json"),
            },
            // (4) Use new syntax when there is no configuration entry available:
            {
                "mtad-05.yaml", "configuration-entries-04.json", new Expectation(Expectation.Type.EXCEPTION, "No configuration entries were found matching the filter specified in resource \"resource-2\"")
            },
            // (5) Use new syntax when there is no configuration entry available:
            {
                "mtad-07.yaml", "configuration-entries-07.json", new Expectation(Expectation.Type.RESOURCE, "result-03.json"),
            },
            // (6) Use new syntax (missing org parameter):
            {
                "mtad-06.yaml", "configuration-entries-01.json", new Expectation(Expectation.Type.EXCEPTION, "Could not find required property \"org\"")
            },
            // (7) Subscriptions should be created:
            {
                "mtad-08.yaml", "configuration-entries-06.json", new Expectation(Expectation.Type.RESOURCE, "result-04.json"),
            },
// @formatter:on
        });
    }

    @BeforeClass
    public static void initializeTargetAndPlatformType() throws Exception {
        ConfigurationParser parser = new ConfigurationParser();
        platform = parser.parsePlatformJson2(ConfigurationReferencesResolverTest.class.getResourceAsStream("/mta/xs-platform-v2.json"));
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

    protected ConfigurationReferencesResolver getConfigurationResolver(DeploymentDescriptor deploymentDescriptor) {
        String currentOrg = (String) platform.getParameters()
            .get("org");
        String currentSpace = (String) platform.getParameters()
            .get("space");
        return new ConfigurationReferencesResolver(dao,
            new ConfigurationFilterParser(new CloudTarget(currentOrg, currentSpace), getPropertiesChainBuilder(descriptor)),
            (org, space) -> SPACE_ID, null, configuration);
    }

    protected ParametersChainBuilder getPropertiesChainBuilder(DeploymentDescriptor descriptor) {
        return new ParametersChainBuilder((DeploymentDescriptor) descriptor, platform);
    }

    protected DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }

}
