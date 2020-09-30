package org.cloudfoundry.multiapps.controller.core.helpers.v2;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.test.Tester;
import org.cloudfoundry.multiapps.common.test.Tester.Expectation;
import org.cloudfoundry.multiapps.common.util.YamlParser;
import org.cloudfoundry.multiapps.controller.core.model.ResolvedConfigurationReference;
import org.cloudfoundry.multiapps.controller.persistence.model.CloudTarget;
import org.cloudfoundry.multiapps.controller.persistence.model.filters.ConfigurationFilter;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorHandler;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorParser;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ConfigurationSubscriptionFactoryTest {

    protected final Tester tester = Tester.forClass(getClass());

    public static Stream<Arguments> testCreate() {
        return Stream.of(
// @formatter:off
            // (0) The required dependency is managed, so a subscription should be created:
            Arguments.of("subscriptions-mtad-00.yaml", List.of("plugins"), "SPACE_ID_1", new Expectation(Expectation.Type.JSON, "subscriptions-00.json")),
            // (1) The required dependency is not managed, so a subscription should not be created:
            Arguments.of("subscriptions-mtad-01.yaml", List.of("plugins"), "SPACE_ID_1", new Expectation("[]")),
            // (2) The required dependency is not managed, so a subscription should not be created:
            Arguments.of("subscriptions-mtad-02.yaml", List.of("plugins"), "SPACE_ID_1", new Expectation("[]"))
// @formatter:on
        );
    }

    private List<String> configurationResources;

    public ConfigurationSubscriptionFactoryTest() {

    }

    @ParameterizedTest
    @MethodSource
    void testCreate(String mtadFilePath, List<String> configurationResources, String spaceId, Expectation expectation) {
        this.configurationResources = configurationResources;
        String mtadString = TestUtil.getResourceAsString(mtadFilePath, getClass());
        Map<String, Object> deploymentDescriptor = new YamlParser().convertYamlToMap(mtadString);
        DeploymentDescriptor mtad = getDescriptorParser().parseDeploymentDescriptor(deploymentDescriptor);
        Map<String, ResolvedConfigurationReference> resolvedResources = getResolvedConfigurationReferences(mtad);
        testCreate(mtad, resolvedResources, spaceId, expectation);
    }

    protected void testCreate(DeploymentDescriptor mtad, Map<String, ResolvedConfigurationReference> resolvedResources, String spaceId,
                              Expectation expectation) {
        tester.test(() -> new ConfigurationSubscriptionFactory(mtad, resolvedResources).create(spaceId), expectation);
    }

    private Map<String, ResolvedConfigurationReference> getResolvedConfigurationReferences(DeploymentDescriptor descriptor) {
        return configurationResources.stream()
                                     .collect(Collectors.toMap(Function.identity(),
                                                               resource -> getResolvedConfigurationReference(descriptor, resource)));
    }

    private ResolvedConfigurationReference getResolvedConfigurationReference(DeploymentDescriptor descriptor,
                                                                             String configurationResource) {
        DescriptorHandler handler = new DescriptorHandler();
        Resource resource = handler.findResource(descriptor, configurationResource);
        return new ResolvedConfigurationReference(createDummyFilter(), resource, Collections.emptyList());
    }

    private ConfigurationFilter createDummyFilter() {
        return new ConfigurationFilter("mta", "com.sap.other.mta", "1.0.0", null, new CloudTarget("ORG", "SPACE"), Collections.emptyMap());
    }

    protected DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }

}
