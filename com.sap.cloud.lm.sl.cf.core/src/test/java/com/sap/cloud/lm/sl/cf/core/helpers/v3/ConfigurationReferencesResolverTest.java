package com.sap.cloud.lm.sl.cf.core.helpers.v3;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationFilterParser;
import com.sap.cloud.lm.sl.common.util.Tester.Expectation;
import com.sap.cloud.lm.sl.mta.handlers.v3.DescriptorParser;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;

public class ConfigurationReferencesResolverTest extends com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationReferencesResolverTest {

    protected static Stream<Arguments> testResolve() {
        return Stream.of(
// @formatter:off
               // (1) Reference to existing provided dependency:
               Arguments.of("mtad-01.yaml", "configuration-entries-01.json", new Expectation(Expectation.Type.JSON, "result-01.json")),
               // (2) More than one configuration entries are available for resource:
               Arguments.of("mtad-02.yaml", "configuration-entries-02.json", new Expectation(Expectation.Type.JSON, "result-02.json")),
               // (3) Set configuration property "active" to false
               Arguments.of("mtad-03.yaml", "configuration-entries-02.json", new Expectation(Expectation.Type.JSON, "result-03.json")) 
// @formatter:on
        );
    }

    @Override
    protected DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }

    @Override
    protected ConfigurationReferencesResolver getConfigurationResolver(DeploymentDescriptor deploymentDescriptor) {
        return new ConfigurationReferencesResolver(configurationEntryService,
                                                   new ConfigurationFilterParser(getCloudTarget(), getPropertiesChainBuilder(descriptor)),
                                                   null,
                                                   configuration);
    }

}
