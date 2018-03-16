package com.sap.cloud.lm.sl.cf.core.helpers.v2_0;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ResourceTypeFinder;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.UserProvidedResourceResolver;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorParser;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Platform;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;

public class UserProvidedResourceResolverTest extends com.sap.cloud.lm.sl.cf.core.helpers.v1_0.UserProvidedResourceResolverTest {

    @Parameters
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            {
                "mtad-09.yaml", "/mta/targets-v2.json", "/mta/platform-types-v2.json", "R:mtad-09.yaml.json",
            },
            {
                "mtad-10.yaml", "/mta/targets-v2.json", "/mta/platform-types-v2.json", "R:mtad-10.yaml.json",
            },
            {
                "mtad-11.yaml", "/mta/targets-v2.json", "/mta/platform-types-v2.json", "R:mtad-11.yaml.json",
            },
// @formatter:on
        });
    }

    public UserProvidedResourceResolverTest(String descriptorLocation, String targetLocation, String platformLocation, String expected) {
        super(descriptorLocation, targetLocation, platformLocation, expected);
    }

    @Override
    protected ConfigurationParser getConfigurationParser() {
        return new com.sap.cloud.lm.sl.mta.handlers.v2_0.ConfigurationParser();
    }

    @Override
    protected DescriptorParser getDescriptorParser() {
        return new com.sap.cloud.lm.sl.mta.handlers.v2_0.DescriptorParser();
    }

    @Override
    protected ResourceTypeFinder getResourceTypeFinder() {
        return new com.sap.cloud.lm.sl.cf.core.helpers.v2_0.ResourceTypeFinder(USER_PROVIDED_SERVICE_TYPE);
    }

    @Override
    protected UserProvidedResourceResolver getUserProidedResourceResolver(DeploymentDescriptor descriptor, Target target,
        Platform platform) {
        return new com.sap.cloud.lm.sl.cf.core.helpers.v2_0.UserProvidedResourceResolver(getResourceTypeFinder(),
            (com.sap.cloud.lm.sl.mta.model.v2_0.DeploymentDescriptor) descriptor, (com.sap.cloud.lm.sl.mta.model.v2_0.Target) target,
            (com.sap.cloud.lm.sl.mta.model.v2_0.Platform) platform);
    }
}
