package com.sap.cloud.lm.sl.cf.core.cf.v3_1;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.CloudModelConfiguration;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.DomainsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v2_0.ApplicationsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v3_1.DeployTargetFactory;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.common.util.Callable;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.handlers.v2_0.DescriptorMerger;
import com.sap.cloud.lm.sl.mta.handlers.v3_1.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.handlers.v3_1.DescriptorParser;
import com.sap.cloud.lm.sl.mta.mergers.v2_0.PlatformMerger;
import com.sap.cloud.lm.sl.mta.mergers.v2_0.TargetMerger;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.ExtensionDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Platform;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;
import com.sap.cloud.lm.sl.mta.resolvers.ResolverBuilder;
import com.sap.cloud.lm.sl.mta.resolvers.v2_0.DescriptorReferenceResolver;

public class CloudModelBuilderTest extends com.sap.cloud.lm.sl.cf.core.cf.v2_0.CloudModelBuilderTest {

    @Mock
    private UserMessageLogger userMessageLogger;

    private ApplicationsCloudModelBuilder appsBuilder;
    private ServicesCloudModelBuilder servicesBuilder;
    private DomainsCloudModelBuilder domainsBuilder;

    public CloudModelBuilderTest(String deploymentDescriptorLocation, String extensionDescriptorLocation, String platformsLocation,
        String targetsLocation, boolean useNamespaces, boolean useNamespacesForServices, boolean allowInvalidEnvNames,
        String[] mtaArchiveModules, String[] mtaModules, String[] deployedApps, String[] expected) {
        super(deploymentDescriptorLocation, extensionDescriptorLocation, platformsLocation, targetsLocation, useNamespaces,
            useNamespacesForServices, allowInvalidEnvNames, mtaArchiveModules, mtaModules, deployedApps, expected);
        MockitoAnnotations.initMocks(this);
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (00) Test missing resource type definition:
            {
                "mtad-missing-resource-type-definition.yaml", "config-01.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                false, false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "[]", "R:apps-01.json", }
            },
// @formatter:on
        });
    }

    @Before
    public void setUp() throws Exception {
        DescriptorParser descriptorParser = getDescriptorParser();
        ConfigurationParser configurationParser = getConfigurationParser();

        InputStream deploymentDescriptorYaml = getClass().getResourceAsStream(deploymentDescriptorLocation);
        DeploymentDescriptor deploymentDescriptor = descriptorParser.parseDeploymentDescriptorYaml(deploymentDescriptorYaml);

        InputStream extensionDescriptorYaml = getClass().getResourceAsStream(extensionDescriptorLocation);
        ExtensionDescriptor extensionDescriptor = descriptorParser.parseExtensionDescriptorYaml(extensionDescriptorYaml);

        InputStream platformsJson = getClass().getResourceAsStream(platformsLocation);
        List<Platform> platforms = configurationParser.parsePlatformsJson(platformsJson);

        InputStream targetsJson = getClass().getResourceAsStream(targetsLocation);
        List<Target> targets = configurationParser.parseTargetsJson(targetsJson);

        DescriptorHandler handler = getDescriptorHandler();

        String targetName = extensionDescriptor.getDeployTargets().get(0);
        Target target = handler.findTarget(targets, targetName, null);
        Platform platform = handler.findPlatform(platforms, target.getType());

        deploymentDescriptor = getDescriptorMerger().merge(deploymentDescriptor, Arrays.asList(extensionDescriptor))._1;
        insertProperAppNames(deploymentDescriptor);

        TargetMerger platformMerger = getTargetMerger(target, handler);
        platformMerger.mergeInto(deploymentDescriptor);

        PlatformMerger platformTypeMerger = getPlatformMerger(platform, handler);
        platformTypeMerger.mergeInto(deploymentDescriptor);

        String defaultDomain = getDefaultDomain(targetName);

        SystemParameters systemParameters = createSystemParameters(deploymentDescriptor, defaultDomain);
        XsPlaceholderResolver xsPlaceholderResolver = new XsPlaceholderResolver();
        xsPlaceholderResolver.setDefaultDomain(defaultDomain);
        CloudModelConfiguration configuration = new CloudModelConfiguration();
        configuration.setPortBasedRouting(defaultDomain.equals(DEFAULT_DOMAIN_XS));
        configuration.setPrettyPrinting(false);
        configuration.setAllowInvalidEnvNames(allowInvalidEnvNames);
        configuration.setUseNamespaces(useNamespaces);
        configuration.setUseNamespacesForServices(useNamespacesForServices);
        appsBuilder = getApplicationsCloudModelBuilder(deploymentDescriptor, systemParameters, xsPlaceholderResolver, configuration);
        domainsBuilder = getDomainsBuilder(deploymentDescriptor, systemParameters, xsPlaceholderResolver);
        servicesBuilder = getServicesCloudModelBuilder(deploymentDescriptor, configuration);
    }

    @Override
    protected DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }

    @Override
    protected ConfigurationParser getConfigurationParser() {
        return new ConfigurationParser();
    }

    @Override
    protected com.sap.cloud.lm.sl.mta.handlers.v3_1.DescriptorHandler getDescriptorHandler() {
        return new com.sap.cloud.lm.sl.mta.handlers.v3_1.DescriptorHandler();
    }

    @Override
    protected DeployTargetFactory getDeployTargetFactory() {
        return new DeployTargetFactory();
    }

    @Override
    protected DescriptorMerger getDescriptorMerger() {
        return new DescriptorMerger();
    }

    @Override
    protected ServicesCloudModelBuilder getServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        CloudModelConfiguration configuration) {
        return new ServicesCloudModelBuilder(deploymentDescriptor,
            new HandlerFactory(2).getPropertiesAccessor(), configuration, userMessageLogger);
    }

    @Override
    protected ApplicationsCloudModelBuilder getApplicationsCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        SystemParameters systemParameters, XsPlaceholderResolver xsPlaceholderResolver, CloudModelConfiguration configuration) {
        deploymentDescriptor = new DescriptorReferenceResolver(
            (com.sap.cloud.lm.sl.mta.model.v3_1.DeploymentDescriptor) deploymentDescriptor, new ResolverBuilder(), new ResolverBuilder())
                .resolve();
        return new com.sap.cloud.lm.sl.cf.core.cf.v2_0.ApplicationsCloudModelBuilder(
            (com.sap.cloud.lm.sl.mta.model.v3_1.DeploymentDescriptor) deploymentDescriptor, configuration, null, systemParameters,
            xsPlaceholderResolver, DEPLOY_ID);
    }

    @Override
    protected TargetMerger getTargetMerger(Target target, DescriptorHandler handler) {
        return new com.sap.cloud.lm.sl.mta.mergers.v2_0.TargetMerger((com.sap.cloud.lm.sl.mta.model.v3_1.Target) target,
            (com.sap.cloud.lm.sl.mta.handlers.v3_1.DescriptorHandler) handler);
    }

    @Override
    protected PlatformMerger getPlatformMerger(Platform platform, DescriptorHandler handler) {
        return new com.sap.cloud.lm.sl.mta.mergers.v2_0.PlatformMerger((com.sap.cloud.lm.sl.mta.model.v3_1.Platform) platform,
            (com.sap.cloud.lm.sl.mta.handlers.v3_1.DescriptorHandler) handler);
    }

    @Test
    public void testWarnMessage() {
        servicesBuilder.build(mtaArchiveModules);
        Mockito.verify(userMessageLogger).warn(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void testGetCustomDomains() {
        TestUtil.test(new Callable<List<String>>() {
            @Override
            public List<String> call() throws Exception {
                return domainsBuilder.build();
            }
        }, expected[0], getClass(), new TestUtil.JsonSerializationOptions(false, true));
    }

    @Test
    public void testGetApplications() {
        TestUtil.test(new Callable<List<CloudApplicationExtended>>() {
            @Override
            public List<CloudApplicationExtended> call() throws Exception {
                return appsBuilder.build(mtaArchiveModules, mtaModules, deployedApps);
            }
        }, expected[2], getClass(), new TestUtil.JsonSerializationOptions(false, true));
    }

    @Test
    public void testGetServices() {
        TestUtil.test(new Callable<List<CloudServiceExtended>>() {
            @Override
            public List<CloudServiceExtended> call() throws Exception {
                return servicesBuilder.build(mtaArchiveModules);
            }
        }, expected[1], getClass(), new TestUtil.JsonSerializationOptions(false, true));
    }
}
