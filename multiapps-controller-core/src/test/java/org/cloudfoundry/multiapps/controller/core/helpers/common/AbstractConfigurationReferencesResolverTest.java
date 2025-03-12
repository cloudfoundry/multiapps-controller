package org.cloudfoundry.multiapps.controller.core.helpers.common;

import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.test.Tester;
import org.cloudfoundry.multiapps.common.test.Tester.Expectation;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.common.util.YamlParser;
import org.cloudfoundry.multiapps.controller.core.helpers.v2.ConfigurationFilterParser;
import org.cloudfoundry.multiapps.controller.core.helpers.v2.ConfigurationReferencesResolver;
import org.cloudfoundry.multiapps.controller.core.test.MockBuilder;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.CloudTarget;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.filters.ConfigurationFilter;
import org.cloudfoundry.multiapps.controller.persistence.query.ConfigurationEntryQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationEntryService;
import org.cloudfoundry.multiapps.mta.builders.v2.ParametersChainBuilder;
import org.cloudfoundry.multiapps.mta.handlers.ConfigurationParser;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorParser;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.type.TypeReference;

public abstract class AbstractConfigurationReferencesResolverTest {
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

    @BeforeAll
    static void initializePlatform() {
        ConfigurationParser parser = new ConfigurationParser();
        platform = parser.parsePlatformJson(AbstractConfigurationReferencesResolverTest.class.getResourceAsStream("/mta/xs-platform.json"));
    }

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    protected void executeTestResolve(Tester tester, String descriptorLocation, String configurationEntriesLocation,
                                      Expectation expectation) {
        prepareService();
        prepareConfigurationEntries(configurationEntriesLocation);
        prepareDeploymentDescriptor(descriptorLocation);
        var referencesResolver = getConfigurationResolver();

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
        for (ServiceMockConfiguration config : serviceConfigurations) {
            ConfigurationFilter filter = config.filter;
            ConfigurationEntryQuery configurationEntryQueryMock = getConfigurationEntryQueryMock(filter);
            when(configurationEntryQueryMock.list()).thenReturn(config.configurationEntries);
        }
    }

    private ConfigurationEntryQuery getConfigurationEntryQueryMock(ConfigurationFilter filter) {
        return new MockBuilder<>(configurationEntryQuery).on(query -> query.providerNid(filter.getProviderNid()))
                                                         .on(query -> query.providerId(filter.getProviderId()))
                                                         .on(query -> query.version(filter.getProviderVersion()))
                                                         .on(query -> query.target(filter.getTargetSpace()))
                                                         .on(query -> query.requiredProperties(filter.getRequiredContent()))
                                                         .on(query -> query.visibilityTargets(Mockito.any()))
                                                         .build();
    }

    protected void prepareDeploymentDescriptor(String descriptorLocation) {
        Map<String, Object> deploymentDescriptorMap = new YamlParser().convertYamlToMap(getClass().getResourceAsStream(descriptorLocation));
        this.descriptor = getDescriptorParser().parseDeploymentDescriptor(deploymentDescriptorMap);
    }

    protected DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }

    protected ConfigurationReferencesResolver getConfigurationResolver() {
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
