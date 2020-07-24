package com.sap.cloud.lm.sl.cf.core.resolvers.v2;

import java.util.Collections;
import java.util.Map;

import org.cloudfoundry.multiapps.common.util.Tester;
import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.cloudfoundry.multiapps.common.util.YamlParser;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorParser;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.junit.Test;

public class PartialDescriptorResolverTest {

    private final Tester tester = Tester.forClass(getClass());

    @Test
    public void testResolve() {
        Map<String, Object> deploymentDescriptorMap = new YamlParser().convertYamlToMap(getClass().getResourceAsStream("mtad.yaml"));
        DeploymentDescriptor descriptor = new DescriptorParser().parseDeploymentDescriptor(deploymentDescriptorMap);
        PartialDescriptorReferenceResolver resolver = new PartialDescriptorReferenceResolver(descriptor,
                                                                                             Collections.singletonList("plugins"));
        tester.test(resolver::resolve, new Expectation(Expectation.Type.JSON, "resolved-mtad.json"));
    }

}
