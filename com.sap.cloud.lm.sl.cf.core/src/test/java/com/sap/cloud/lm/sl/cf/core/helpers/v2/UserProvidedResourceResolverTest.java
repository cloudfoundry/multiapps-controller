package com.sap.cloud.lm.sl.cf.core.helpers.v2;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.helpers.v1.ResourceTypeFinder;
import com.sap.cloud.lm.sl.cf.core.helpers.v1.UserProvidedResourceResolver;
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;
import com.sap.cloud.lm.sl.mta.handlers.v1.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.handlers.v1.DescriptorParser;
import com.sap.cloud.lm.sl.mta.model.v1.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1.Platform;
import com.sap.cloud.lm.sl.mta.model.v1.Target;

public class UserProvidedResourceResolverTest extends com.sap.cloud.lm.sl.cf.core.helpers.v1.UserProvidedResourceResolverTest {

    @Parameters
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            {
                "mtad-09.yaml", "/mta/targets-v2.json", "/mta/platform-types-v2.json", new Expectation(Expectation.Type.RESOURCE, "mtad-09.yaml.json"),
            },
            {
                "mtad-10.yaml", "/mta/targets-v2.json", "/mta/platform-types-v2.json", new Expectation(Expectation.Type.RESOURCE, "mtad-10.yaml.json"),
            },
            {
                "mtad-11.yaml", "/mta/targets-v2.json", "/mta/platform-types-v2.json", new Expectation(Expectation.Type.RESOURCE, "mtad-11.yaml.json"),
            },
// @formatter:on
        });
    }

    public UserProvidedResourceResolverTest(String descriptorLocation, String targetLocation, String platformLocation, Expectation expectation) {
        super(descriptorLocation, targetLocation, platformLocation, expectation);
    }

    @Override
    protected ConfigurationParser getConfigurationParser() {
        return new com.sap.cloud.lm.sl.mta.handlers.v2.ConfigurationParser();
    }

    @Override
    protected DescriptorParser getDescriptorParser() {
        return new com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorParser();
    }

    @Override
    protected ResourceTypeFinder getResourceTypeFinder() {
        return new com.sap.cloud.lm.sl.cf.core.helpers.v2.ResourceTypeFinder(USER_PROVIDED_SERVICE_TYPE);
    }

    @Override
    protected UserProvidedResourceResolver getUserProidedResourceResolver(DeploymentDescriptor descriptor, Target target,
        Platform platform) {
        return new com.sap.cloud.lm.sl.cf.core.helpers.v2.UserProvidedResourceResolver(getResourceTypeFinder(),
            (com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor) descriptor, (com.sap.cloud.lm.sl.mta.model.v2.Target) target,
            (com.sap.cloud.lm.sl.mta.model.v2.Platform) platform);
    }
}
