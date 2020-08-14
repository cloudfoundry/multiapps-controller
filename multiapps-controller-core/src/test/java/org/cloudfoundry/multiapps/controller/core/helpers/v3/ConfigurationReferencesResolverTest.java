package org.cloudfoundry.multiapps.controller.core.helpers.v3;

import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.cloudfoundry.multiapps.controller.core.helpers.v2.ConfigurationFilterParser;
import org.cloudfoundry.multiapps.mta.handlers.v3.DescriptorParser;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.junit.jupiter.params.provider.Arguments;

public class ConfigurationReferencesResolverTest
    extends org.cloudfoundry.multiapps.controller.core.helpers.v2.ConfigurationReferencesResolverTest {

    static Stream<Arguments> testResolve() {
        return Stream.of(
                         // (1) Reference to existing provided dependency:
                         Arguments.of("mtad-01.yaml", "configuration-entries-01.json",
                                      new Expectation(Expectation.Type.JSON, "result-01.json")),
                         // (2) More than one configuration entries are available for resource:
                         Arguments.of("mtad-02.yaml", "configuration-entries-02.json",
                                      new Expectation(Expectation.Type.JSON, "result-02.json")),
                         // (3) Set configuration property "active" to false
                         Arguments.of("mtad-03.yaml", "configuration-entries-02.json",
                                      new Expectation(Expectation.Type.JSON, "result-03.json")));
    }

    @Override
    protected DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }

    @Override
    protected ConfigurationReferencesResolver getConfigurationResolver(DeploymentDescriptor deploymentDescriptor) {
        return new ConfigurationReferencesResolver(configurationEntryService,
                                                   new ConfigurationFilterParser(getCloudTarget(),
                                                                                 getPropertiesChainBuilder(descriptor),
                                                                                 null),
                                                   null,
                                                   configuration);
    }

}
