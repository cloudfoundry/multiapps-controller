package com.sap.cloud.lm.sl.cf.core.helpers.v1;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.cf.v1.ResourceType;
import com.sap.cloud.lm.sl.cf.core.helpers.v1.ResourceTypeFinder;
import com.sap.cloud.lm.sl.cf.core.helpers.v1.UserProvidedResourceResolver;
import com.sap.cloud.lm.sl.common.util.Callable;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.mta.handlers.v1.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.handlers.v1.DescriptorParser;
import com.sap.cloud.lm.sl.mta.model.v1.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1.Platform;
import com.sap.cloud.lm.sl.mta.model.v1.Target;

@RunWith(Parameterized.class)
public class UserProvidedResourceResolverTest {

    protected static final String USER_PROVIDED_SERVICE_TYPE = ResourceType.USER_PROVIDED_SERVICE.toString();

    private String descriptorLocation;
    private String targetLocation;
    private String platformLocation;
    private String expectedJsonLocation;

    protected UserProvidedResourceResolver resolver;

    @Parameters
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            {
                "mtad-07.yaml", "/mta/targets.json", "/mta/platform-types.json", "R:mtad-07.yaml.json",
            },
            {
                "mtad-08.yaml", "/mta/targets.json", "/mta/platform-types.json", "R:mtad-08.yaml.json",
            },
            {
                "mtad-09.yaml", "/mta/targets.json", "/mta/platform-types.json", "R:mtad-09.yaml.json",
            },
// @formatter:on
        });
    }

    public UserProvidedResourceResolverTest(String descriptorLocation, String platformLocation, String platformTypeLocation,
        String expected) {
        this.descriptorLocation = descriptorLocation;
        this.targetLocation = platformLocation;
        this.platformLocation = platformTypeLocation;
        this.expectedJsonLocation = expected;
    }

    @Before
    public void setUp() throws Exception {
        DescriptorParser descriptorParser = getDescriptorParser();
        ConfigurationParser configurationParser = getConfigurationParser();

        InputStream descriptorYaml = getClass().getResourceAsStream(descriptorLocation);
        DeploymentDescriptor descriptor = descriptorParser.parseDeploymentDescriptorYaml(descriptorYaml);

        InputStream targetJson = getClass().getResourceAsStream(targetLocation);
        Target target = configurationParser.parseTargetsJson(targetJson)
            .get(2);

        InputStream platformJson = getClass().getResourceAsStream(platformLocation);
        Platform platform = configurationParser.parsePlatformsJson(platformJson)
            .get(0);

        resolver = getUserProidedResourceResolver(descriptor, target, platform);

    }

    @Test
    public void testResolve() {
        TestUtil.test(new Callable<DeploymentDescriptor>() {

            @Override
            public DeploymentDescriptor call() throws Exception {
                return resolver.resolve();
            }
        }, expectedJsonLocation, getClass());
    }

    protected UserProvidedResourceResolver getUserProidedResourceResolver(DeploymentDescriptor descriptor, Target target,
        Platform platform) {
        return new UserProvidedResourceResolver(getResourceTypeFinder(), descriptor, target, platform);
    }

    protected ResourceTypeFinder getResourceTypeFinder() {
        return new ResourceTypeFinder(USER_PROVIDED_SERVICE_TYPE);
    }

    protected ConfigurationParser getConfigurationParser() {
        return new ConfigurationParser();
    }

    protected DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }
}
