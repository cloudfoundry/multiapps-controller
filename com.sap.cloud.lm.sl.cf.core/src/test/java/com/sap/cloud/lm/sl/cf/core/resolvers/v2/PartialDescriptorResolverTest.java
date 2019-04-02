package com.sap.cloud.lm.sl.cf.core.resolvers.v2;

import java.util.Arrays;

import org.junit.Test;

import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorParser;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;

public class PartialDescriptorResolverTest {

    @Test
    public void testResolve() {
        DeploymentDescriptor descriptor = new DescriptorParser().parseDeploymentDescriptorYaml(getClass().getResourceAsStream("mtad.yaml"));
        PartialDescriptorReferenceResolver resolver = new PartialDescriptorReferenceResolver(descriptor, Arrays.asList("plugins"));
        TestUtil.test(() -> resolver.resolve(), new Expectation(Expectation.Type.RESOURCE, "resolved-mtad.json"), getClass());
    }

}
