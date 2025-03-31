package org.cloudfoundry.multiapps.controller.core.helpers.common;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.test.Tester;
import org.cloudfoundry.multiapps.common.util.YamlParser;
import org.cloudfoundry.multiapps.controller.core.helpers.v2.ConfigurationSubscriptionFactory;
import org.cloudfoundry.multiapps.controller.core.model.ResolvedConfigurationReference;
import org.cloudfoundry.multiapps.controller.persistence.model.CloudTarget;
import org.cloudfoundry.multiapps.controller.persistence.model.filters.ConfigurationFilter;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorHandler;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorParser;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;

public class AbstractConfigurationSubscriptionFactoryTest {

    private List<String> configurationResources;

    protected void executeTestCreate(Tester tester, String mtadFilePath, List<String> configurationResources, String spaceId,
                                     Tester.Expectation expectation) {
        this.configurationResources = configurationResources;
        String mtadString = TestUtil.getResourceAsString(mtadFilePath, getClass());
        Map<String, Object> deploymentDescriptor = new YamlParser().convertYamlToMap(mtadString);
        DeploymentDescriptor mtad = getDescriptorParser().parseDeploymentDescriptor(deploymentDescriptor);
        Map<String, ResolvedConfigurationReference> resolvedResources = getResolvedConfigurationReferences(mtad);
        testCreate(tester, mtad, resolvedResources, spaceId, expectation);
    }

    private void testCreate(Tester tester, DeploymentDescriptor mtad, Map<String, ResolvedConfigurationReference> resolvedResources,
                            String spaceId, Tester.Expectation expectation) {
        tester.test(() -> new ConfigurationSubscriptionFactory(mtad, resolvedResources, Collections.emptySet()).create(spaceId),
                    expectation);
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
