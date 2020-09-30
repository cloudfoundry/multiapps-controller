package org.cloudfoundry.multiapps.controller.core.resolvers.v2;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.common.test.Tester;
import org.cloudfoundry.multiapps.common.test.Tester.Expectation;
import org.cloudfoundry.multiapps.common.util.YamlParser;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorParser;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.junit.jupiter.api.Test;

class PartialDescriptorResolverTest {

    private final Tester tester = Tester.forClass(getClass());

    @Test
    void testResolve() {
        Map<String, Object> deploymentDescriptorMap = new YamlParser().convertYamlToMap(getClass().getResourceAsStream("mtad.yaml"));
        DeploymentDescriptor descriptor = new DescriptorParser().parseDeploymentDescriptor(deploymentDescriptorMap);
        PartialDescriptorReferenceResolver resolver = new PartialDescriptorReferenceResolver(descriptor, List.of("plugins"));
        tester.test(resolver::resolve, new Expectation(Expectation.Type.JSON, "resolved-mtad.json"));
    }

}
