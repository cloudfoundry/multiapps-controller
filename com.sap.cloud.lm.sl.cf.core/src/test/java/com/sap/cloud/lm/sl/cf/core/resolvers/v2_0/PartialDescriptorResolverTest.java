package com.sap.cloud.lm.sl.cf.core.resolvers.v2_0;

import java.util.Arrays;

import org.junit.Test;

import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.mta.handlers.v2_0.DescriptorParser;
import com.sap.cloud.lm.sl.mta.model.v2_0.DeploymentDescriptor;

public class PartialDescriptorResolverTest {

    @Test
    public void testResolve() {
        DeploymentDescriptor descriptor = new DescriptorParser().parseDeploymentDescriptorYaml(getClass().getResourceAsStream("mtad.yaml"));
        PartialDescriptorReferenceResolver resolver = new PartialDescriptorReferenceResolver(descriptor, Arrays.asList("plugins"));
        TestUtil.test(() -> {
            return resolver.resolve();
        } , "R:resolved-mtad.json", getClass());
    }

}
