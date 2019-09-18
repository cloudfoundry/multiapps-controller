package com.sap.cloud.lm.sl.cf.core.helpers.v2;

import static com.sap.cloud.lm.sl.common.util.TestUtil.getResourceAsString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationFilter;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.Tester;
import com.sap.cloud.lm.sl.common.util.Tester.Expectation;
import com.sap.cloud.lm.sl.mta.builders.v2.ParametersChainBuilder;
import com.sap.cloud.lm.sl.mta.handlers.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorParser;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Platform;

public class ConfigurationReferencesResolverTest {

    private final Tester tester = Tester.forClass(getClass());

    protected static class DaoMockConfiguration {

        ConfigurationFilter filter;
        List<ConfigurationEntry> configurationEntries;

    }

    private static Platform platform;

    @Mock
    protected ConfigurationEntryDao dao;
    @Mock
    protected ApplicationConfiguration configuration;

    protected DeploymentDescriptor descriptor;

    protected static Stream<Arguments> testResolve() {
        return Stream.of(
// @formatter:off
            // (1) Reference to existing provided dependency:
            Arguments.of("mtad-03.yaml", "configuration-entries-01.json", new Expectation(Expectation.Type.JSON, "result-01.json")),        
            // (2) Use new syntax:
            Arguments.of("mtad-05.yaml", "configuration-entries-01.json", new Expectation(Expectation.Type.JSON, "result-01.json")),            
            // (3) Use new syntax when more than one configuration entries are available:
            Arguments.of("mtad-05.yaml", 
                         "configuration-entries-05.json", 
                         new Expectation(Expectation.Type.EXCEPTION, "Multiple configuration entries were found matching the filter specified in resource \"resource-2\"")),            
            // (4) Use new syntax when more than one configuration entries are available:
            Arguments.of("mtad-07.yaml", "configuration-entries-06.json", new Expectation(Expectation.Type.JSON, "result-02.json")),          
            // (5) Use new syntax when there is no configuration entry available:
            Arguments.of("mtad-05.yaml", 
                         "configuration-entries-04.json", 
                         new Expectation(Expectation.Type.EXCEPTION, "No configuration entries were found matching the filter specified in resource \"resource-2\"")),
            // (6) Use new syntax when there is no configuration entry available:
            Arguments.of("mtad-07.yaml", "configuration-entries-07.json", new Expectation(Expectation.Type.JSON, "result-03.json")),
            // (7) Use new syntax (missing org parameter):
            Arguments.of("mtad-06.yaml", "configuration-entries-01.json", new Expectation(Expectation.Type.EXCEPTION, "Could not find required property \"org\"")),
            // (8) Subscriptions should be created:
            Arguments.of("mtad-08.yaml", "configuration-entries-06.json", new Expectation(Expectation.Type.JSON, "result-04.json"))
// @formatter:on
        );
    }

    @BeforeAll
    public static void initializePlatform() throws Exception {
        ConfigurationParser parser = new ConfigurationParser();
        platform = parser.parsePlatformJson(ConfigurationReferencesResolverTest.class.getResourceAsStream("/mta/xs-platform.json"));
    }

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @ParameterizedTest
    @MethodSource
    public void testResolve(String descriptorLocation, String configurationEntriesLocation, Expectation expectation) {
        prepareConfigurationEntries(configurationEntriesLocation);
        prepareDeploymentDescriptor(descriptorLocation);
        ConfigurationReferencesResolver referencesResolver = getConfigurationResolver(descriptor);

        tester.test(() -> {

            referencesResolver.resolve(descriptor);
            return descriptor;

        }, expectation);
    }

    protected void prepareConfigurationEntries(String configurationEntriesLocation) {
        List<DaoMockConfiguration> daoConfigurations = JsonUtil.fromJson(getResourceAsString(configurationEntriesLocation, getClass()),
                                                                         new TypeReference<List<DaoMockConfiguration>>() {
                                                                         });
        for (DaoMockConfiguration configuration : daoConfigurations) {
            ConfigurationFilter filter = configuration.filter;
            when(dao.find(filter.getProviderNid(), filter.getProviderId(), filter.getProviderVersion(), filter.getTargetSpace(),
                          filter.getRequiredContent(), null, null)).thenReturn(configuration.configurationEntries);
        }
    }

    protected void prepareDeploymentDescriptor(String descriptorLocation) {
        this.descriptor = getDescriptorParser().parseDeploymentDescriptorYaml(getClass().getResourceAsStream(descriptorLocation));
    }

    protected DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }

    protected ConfigurationReferencesResolver getConfigurationResolver(DeploymentDescriptor deploymentDescriptor) {

        return new ConfigurationReferencesResolver(dao,
                                                   new ConfigurationFilterParser(getCloudTarget(), getPropertiesChainBuilder(descriptor)),
                                                   null,
                                                   configuration);
    }

    protected CloudTarget getCloudTarget() {
        String currentOrg = (String) platform.getParameters()
                                             .get("org");
        String currentSpace = (String) platform.getParameters()
                                               .get("space");
        return new CloudTarget(currentOrg, currentSpace);
    }

    protected ParametersChainBuilder getPropertiesChainBuilder(DeploymentDescriptor descriptor) {
        return new ParametersChainBuilder(descriptor, platform);
    }

}
