package org.cloudfoundry.multiapps.controller.core.helpers.v2;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.common.util.TestUtil;
import org.cloudfoundry.multiapps.common.util.Tester;
import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.cloudfoundry.multiapps.common.util.YamlParser;
import org.cloudfoundry.multiapps.controller.core.model.CloudTarget;
import org.cloudfoundry.multiapps.controller.core.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.controller.core.model.ConfigurationFilter;
import org.cloudfoundry.multiapps.controller.core.persistence.query.ConfigurationEntryQuery;
import org.cloudfoundry.multiapps.controller.core.persistence.service.ConfigurationEntryService;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.MockBuilder;
import org.cloudfoundry.multiapps.mta.builders.v2.ParametersChainBuilder;
import org.cloudfoundry.multiapps.mta.handlers.ConfigurationParser;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorParser;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.type.TypeReference;

public class ConfigurationReferencesResolverTest {

    private final Tester tester = Tester.forClass(getClass());

    protected static class ServiceMockConfiguration {

        ConfigurationFilter filter;
        List<ConfigurationEntry> configurationEntries;

    }

    private static Platform platform;

    @Mock
    protected ConfigurationEntryService configurationEntryService;
    @Mock(answer = Answers.RETURNS_SELF)
    protected ConfigurationEntryQuery configurationEntryQuery;
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
    public static void initializePlatform() {
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
        prepareService();
        prepareConfigurationEntries(configurationEntriesLocation);
        prepareDeploymentDescriptor(descriptorLocation);
        ConfigurationReferencesResolver referencesResolver = getConfigurationResolver(descriptor);

        tester.test(() -> {

            referencesResolver.resolve(descriptor);
            return descriptor;

        }, expectation);
    }

    protected void prepareService() {
        when(configurationEntryService.createQuery()).thenReturn(configurationEntryQuery);
    }

    protected void prepareConfigurationEntries(String configurationEntriesLocation) {
        List<ServiceMockConfiguration> serviceConfigurations = JsonUtil.fromJson(TestUtil.getResourceAsString(configurationEntriesLocation,
                                                                                                              getClass()),
                                                                                 new TypeReference<List<ServiceMockConfiguration>>() {
                                                                                 });
        for (ServiceMockConfiguration configuration : serviceConfigurations) {
            ConfigurationFilter filter = configuration.filter;
            ConfigurationEntryQuery configurationEntryQueryMock = getConfigurationEntryQueryMock(filter);
            when(configurationEntryQueryMock.list()).thenReturn(configuration.configurationEntries);
        }
    }

    private ConfigurationEntryQuery getConfigurationEntryQueryMock(ConfigurationFilter filter) {
        return new MockBuilder<>(configurationEntryQuery).on(query -> query.providerNid(filter.getProviderNid()))
                                                         .on(query -> query.providerId(filter.getProviderId()))
                                                         .on(query -> query.version(filter.getProviderVersion()))
                                                         .on(query -> query.target(filter.getTargetSpace()))
                                                         .on(query -> query.requiredProperties(filter.getRequiredContent()))
                                                         .on(query -> query.visibilityTargets(any()))
                                                         .build();
    }

    protected void prepareDeploymentDescriptor(String descriptorLocation) {
        Map<String, Object> deploymentDescriptorMap = new YamlParser().convertYamlToMap(getClass().getResourceAsStream(descriptorLocation));
        this.descriptor = getDescriptorParser().parseDeploymentDescriptor(deploymentDescriptorMap);
    }

    protected DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }

    protected ConfigurationReferencesResolver getConfigurationResolver(DeploymentDescriptor deploymentDescriptor) {
        return new ConfigurationReferencesResolver(configurationEntryService,
                                                   new ConfigurationFilterParser(getCloudTarget(),
                                                                                 getPropertiesChainBuilder(descriptor),
                                                                                 null),
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
