package com.sap.cloud.lm.sl.cf.core.resolvers.v2;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import com.sap.cloud.lm.sl.common.util.Tester;
import com.sap.cloud.lm.sl.common.util.Tester.Expectation;
import com.sap.cloud.lm.sl.common.util.YamlParser;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorParser;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;

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
