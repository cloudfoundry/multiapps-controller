package com.sap.cloud.lm.sl.cf.core.cf.v1_0;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.DeployTargetFactory;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.common.util.Callable;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorMerger;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorParser;
import com.sap.cloud.lm.sl.mta.mergers.v1_0.PlatformMerger;
import com.sap.cloud.lm.sl.mta.mergers.v1_0.TargetMerger;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.ExtensionDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Module;
import com.sap.cloud.lm.sl.mta.model.v1_0.Platform;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;

@RunWith(value = Parameterized.class)
public class CloudModelBuilderTest {

    protected static final String DEFAULT_DOMAIN_CF = "cfapps.neo.ondemand.com";
    protected static final String DEFAULT_DOMAIN_XS = "sofd60245639a";

    protected static final String DEPLOY_ID = "123";

    protected final String deploymentDescriptorLocation;
    protected final String extensionDescriptorLocation;
    protected final String platformsLocation;
    protected final String targetsLocation;
    protected final boolean useNamespaces;
    protected final boolean useNamespacesForServices;
    protected final Set<String> mtaArchiveModules;
    protected final Set<String> mtaModules;
    protected final Set<String> deployedApps;
    protected final String[] expected;

    protected ApplicationsCloudModelBuilder appsBuilder;
    protected ServicesCloudModelBuilder servicesBuilder;
    protected DomainsCloudModelBuilder domainsBuilder;

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (00) Full MTA:
            { "/mta/javahelloworld/mtad.yaml", "/mta/javahelloworld/config1.mtaext", "/mta/platform-types.json", "/mta/targets.json",
                false, false,
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/javahelloworld/services.json",
                    "R:/mta/javahelloworld/apps.json", } },
            // (01)
            { "/mta/javahelloworld/mtad.yaml", "/mta/javahelloworld/xs2-config1.mtaext", "/mta/platform-types.json", "/mta/targets.json",
                false, false,
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/javahelloworld/xs2-services.json",
                    "R:/mta/javahelloworld/xs2-apps.json", } },
            // (02) Full MTA with namespaces:
            { "/mta/javahelloworld/mtad.yaml", "/mta/javahelloworld/config1.mtaext", "/mta/platform-types.json", "/mta/targets.json",
                true, true,
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/javahelloworld/services-ns.json",
                    "R:/mta/javahelloworld/apps-ns.json", } },
            // (03) Full MTA with namespaces (w/o services):
            { "/mta/javahelloworld/mtad.yaml", "/mta/javahelloworld/config1.mtaext", "/mta/platform-types.json", "/mta/targets.json",
                true, false,
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/javahelloworld/services.json",
                    "R:/mta/javahelloworld/apps-ns2.json", } },
            // (04) Patch MTA (resolved inter-module dependencies):
            { "/mta/javahelloworld/mtad.yaml", "/mta/javahelloworld/config1.mtaext", "/mta/platform-types.json", "/mta/targets.json",
                false, false,
                new String[] { "java-hello-world" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/javahelloworld/services-patch1.json",
                    "R:/mta/javahelloworld/apps-patch2.json", } },
            // (05) Patch MTA with namespaces (resolved inter-module dependencies):
            { "/mta/javahelloworld/mtad.yaml", "/mta/javahelloworld/config1.mtaext", "/mta/platform-types.json", "/mta/targets.json",
                true, true,
                new String[] { "java-hello-world" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/javahelloworld/services-patch1-ns.json",
                    "R:/mta/javahelloworld/apps-patch2-ns.json", } },
            // (06) Patch MTA (unresolved inter-module dependencies):
            { "/mta/javahelloworld/mtad.yaml", "/mta/javahelloworld/config1.mtaext", "/mta/platform-types.json", "/mta/targets.json",
                false, false,
                new String[] { "java-hello-world" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] { "java-hello-world", }, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/javahelloworld/services-patch1.json",
                    "E:Unresolved MTA modules [java-hello-world-backend, java-hello-world-db]", } },
            // (07) Patch MTA (module is in archive, but not intended for platform):
            { "/mta/javahelloworld/mtad.yaml", "/mta/javahelloworld/config1.mtaext", "/mta/platform-types.json", "/mta/targets.json",
                false, false,
                new String[] { "java-hello-world", }, // mtaArchiveModules
                new String[] {}, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                "[]", // domains
                "R:/mta/javahelloworld/services-patch1.json",
                "E:MTA module \"java-hello-world\" is part of MTA archive, but is not intended for deployment", } },
            // (08)
            { "/mta/shine/mtad.yaml", "/mta/shine/config1.mtaext", "/mta/platform-types.json", "/mta/targets.json",
                false, false,
                new String[] { "shine", "shine-xsjs", "shine-odata" }, // mtaArchiveModules
                new String[] { "shine", "shine-xsjs", "shine-odata" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/shine/services.json",
                    "R:/mta/shine/apps.json", } },
            // (09)
            { "/mta/sample/mtad.yaml", "/mta/sample/config1.mtaext", "/mta/sample/platform-types.json", "/mta/sample/targets.json",
                false, false,
                new String[] { "pricing", "pricing-db", "web-server" }, // mtaArchiveModules
                new String[] { "pricing", "pricing-db", "web-server" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[\"bestprice.sap.com\"]", // domains
                    "R:/mta/sample/services.json",
                    "R:/mta/sample/apps.json", } },
            // (10)
            { "/mta/devxwebide/mtad.yaml", "/mta/devxwebide/config1.mtaext", "/mta/platform-types.json", "/mta/targets.json",
                false, false,
                new String[] { "webide" }, // mtaArchiveModules
                new String[] { "webide" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/devxwebide/services.json",
                    "R:/mta/devxwebide/apps2.json", } },
            // (11)
            { "/mta/devxwebide/mtad.yaml", "/mta/devxwebide/xs2-config1.mtaext", "/mta/platform-types.json", "/mta/targets.json", 
                false, false,
                new String[] { "webide" }, // mtaArchiveModules
                new String[] { "webide" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/devxwebide/services.json",
                    "R:/mta/devxwebide/xs2-apps2.json", } },
            // (12)
            { "/mta/devxdi/mtad.yaml", "/mta/devxdi/config1.mtaext", "/mta/platform-types.json", "/mta/targets.json",
                false, false,
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaArchiveModules
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/devxdi/services.json",
                    "R:/mta/devxdi/apps.json", } },
            // (13)
            { "/mta/devxdi/mtad.yaml", "/mta/devxdi/xs2-config1.mtaext", "/mta/platform-types.json", "/mta/targets.json",
                false, false,
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaArchiveModules
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/devxdi/xs2-services.json",
                    "R:/mta/devxdi/xs2-apps.json", } },
            // (14)
            { "/mta/devxwebide/mtad.yaml", "/mta/devxwebide/xs2-config2.mtaext", "/mta/platform-types.json", "/mta/targets.json",
                false, false,
                new String[] { "webide" }, // mtaArchiveModules
                new String[] { "webide" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/devxwebide/services.json",
                    "R:/mta/devxwebide/xs2-apps2.json", } },
            // (15) Unknown typed resource properties:
            { "/mta/devxdi/mtad.yaml", "/mta/devxdi/xs2-config2.mtaext", "/mta/platform-types.json", "/mta/targets.json",
                false, false,
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaArchiveModules
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/devxdi/xs2-services.json",
                    "R:/mta/devxdi/xs2-apps.json", } },
            // (16) Custom application names are used:
            {
                "mtad-01.yaml", "config-01.mtaext", "/mta/platform-types.json", "/mta/targets.json",
                false, false,
                new String[] { "module-1", "module-2" }, // mtaArchiveModules
                new String[] { "module-1", "module-2" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "[]", "R:apps-01.json", }
            },
            // (17) Custom application names are used:
            {
                "mtad-01.yaml", "config-01.mtaext", "/mta/platform-types.json", "/mta/targets.json",
                true, true,
                new String[] { "module-1", "module-2" }, // mtaArchiveModules
                new String[] { "module-1", "module-2" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "[]", "R:apps-02.json", }
            },
            // (18) Temporary URIs are used:
            {
                "mtad-03.yaml", "config-01.mtaext", "/mta/platform-types.json", "/mta/targets.json",
                false, false,
                new String[] { "module-1", "module-2" }, // mtaArchiveModules
                new String[] { "module-1", "module-2" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "[]", "R:apps-04.json", }
            },
            // (19) Some env values have HTML symbols embedded in them:
            {
                "mtad-04.yaml", "config-01.mtaext", "/mta/platform-types.json", "/mta/targets.json",
                false, false,
                new String[] { "module-1" }, // mtaArchiveModules
                new String[] { "module-1" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "[]", "R:apps-05.json", }
            },
            // (20) Resource service-name definition in extension descriptor:
            { 
                "mtad-05.yaml", "config-02.mtaext", "/mta/platform-types.json", "/mta/targets.json",
                true, true,
                new String[] { "module-1" }, // mtaArchiveModules
                new String[] { "module-1" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "R:services-02.json", "R:apps-06.json", }
            },
            // (21) Test support for one-off tasks:
            { 
                "mtad-06.yaml", "config-02.mtaext", "/mta/platform-types.json", "/mta/targets.json",
                false, false,
                new String[] { "module-1", "module-2", "module-3" }, // mtaArchiveModules
                new String[] { "module-1", "module-2", "module-3" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "[]", "R:apps-07.json", }
            },
            // (22) Test inject service keys in application environment
            { 
                "mtad-11.yaml", "config-01.mtaext", "/mta/platform-types.json", "/mta/targets.json",
                false, false,
                new String[] { "module-1" }, // mtaArchiveModules
                new String[] { "module-1" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "[]", "R:apps-08.json", }
            },
            // (23) With 'health-check-type' set to 'port':
            { 
                "mtad-health-check-type-port.yaml", "config-02.mtaext", "/mta/platform-types.json", "/mta/targets.json",
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "[]", "R:apps-with-health-check-type-port.json", }
            },
            // (24) With 'health-check-type' set to 'http' and a non-default 'health-check-http-endpoint':
            { 
                "mtad-health-check-type-http-with-endpoint.yaml", "config-02.mtaext", "/mta/platform-types.json", "/mta/targets.json",
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "[]", "R:apps-with-health-check-type-http-with-endpoint.json", }
            },
            // (25) With 'health-check-type' set to 'http' and no 'health-check-http-endpoint':
            { 
                "mtad-health-check-type-http-without-endpoint.yaml", "config-02.mtaext", "/mta/platform-types.json", "/mta/targets.json",
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "[]", "R:apps-with-health-check-type-http-without-endpoint.json", }
            },
            // (26) With 'enable-ssh' set to true: 
            {
                "mtad-ssh-enabled-true.yaml", "config-02.mtaext", "/mta/platform-types.json", "/mta/targets.json",
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "[]", "R:apps-with-ssh-enabled-true.json", }
            },
            // (27) With 'enable-ssh' set to false: 
            {
                "mtad-ssh-enabled-false.yaml", "config-02.mtaext", "/mta/platform-types.json", "/mta/targets.json",
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "[]", "R:apps-with-ssh-enabled-false.json", }
            },
// @formatter:on
        });
    }

    public CloudModelBuilderTest(String deploymentDescriptorLocation, String extensionDescriptorLocation, String platformTypesLocation,
        String platformsLocation, boolean useNamespaces, boolean useNamespacesForServices, String[] mtaArchiveModules, String[] mtaModules,
        String[] deployedApps, String[] expected) {
        this.deploymentDescriptorLocation = deploymentDescriptorLocation;
        this.extensionDescriptorLocation = extensionDescriptorLocation;
        System.out.println(extensionDescriptorLocation);
        this.platformsLocation = platformTypesLocation;
        this.targetsLocation = platformsLocation;
        this.useNamespaces = useNamespaces;
        this.useNamespacesForServices = useNamespacesForServices;
        this.mtaArchiveModules = new HashSet<String>(Arrays.asList(mtaArchiveModules));
        this.mtaModules = new HashSet<String>(Arrays.asList(mtaModules));
        this.deployedApps = new HashSet<String>(Arrays.asList(deployedApps));
        this.expected = expected;
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
        configuration.setUseNamespaces(useNamespaces);
        configuration.setUseNamespacesForServices(useNamespacesForServices);
        appsBuilder = getApplicationsCloudModelBuilder(deploymentDescriptor, systemParameters, xsPlaceholderResolver, configuration);
        domainsBuilder = getDomainsBuilder(deploymentDescriptor, systemParameters, xsPlaceholderResolver);
        servicesBuilder = getServicesCloudModelBuilder(deploymentDescriptor, configuration);
    }

    protected ServicesCloudModelBuilder getServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        CloudModelConfiguration configuration) {
        return new ServicesCloudModelBuilder(deploymentDescriptor, new HandlerFactory(1).getPropertiesAccessor(), configuration);
    }

    protected ApplicationsCloudModelBuilder getApplicationsCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        SystemParameters systemParameters, XsPlaceholderResolver xsPlaceholderResolver, CloudModelConfiguration configuration) {
        return new ApplicationsCloudModelBuilder(deploymentDescriptor, configuration, null, systemParameters, xsPlaceholderResolver,
            DEPLOY_ID);
    }

    protected DomainsCloudModelBuilder getDomainsBuilder(DeploymentDescriptor deploymentDescriptor, SystemParameters systemParameters,
        XsPlaceholderResolver xsPlaceholderResolver) {
        return new DomainsCloudModelBuilder(systemParameters, xsPlaceholderResolver, deploymentDescriptor);
    }

    protected String getDefaultDomain(String targetName) {
        return (targetName.startsWith("CF")) ? DEFAULT_DOMAIN_CF : DEFAULT_DOMAIN_XS;
    }

    protected SystemParameters createSystemParameters(DeploymentDescriptor descriptor, String defaultDomain) {
        Map<String, Object> generalParameters = new HashMap<>();
        generalParameters.put(SupportedParameters.DEFAULT_DOMAIN, defaultDomain);
        Map<String, Map<String, Object>> moduleParameters = new HashMap<>();
        for (Module module : descriptor.getModules1_0()) {
            String moduleName = module.getName();
            moduleParameters.put(moduleName, MapUtil.asMap(SupportedParameters.DEFAULT_HOST, moduleName));
        }
        return new SystemParameters(generalParameters, moduleParameters, Collections.emptyMap(), Collections.emptyMap());
    }

    protected void insertProperAppNames(DeploymentDescriptor descriptor) throws Exception {
        for (Module module : descriptor.getModules1_0()) {
            Map<String, Object> parameters = new TreeMap<>(getParameters(module));
            String appName = NameUtil.getApplicationName(module.getName(), descriptor.getId(), useNamespaces);
            if (parameters.containsKey(SupportedParameters.APP_NAME)) {
                appName = NameUtil.getApplicationName((String) parameters.get(SupportedParameters.APP_NAME), descriptor.getId(),
                    useNamespaces);
            }
            parameters.put(SupportedParameters.APP_NAME, appName);
            setParameters(module, parameters);
        }
    }

    protected DescriptorHandler getDescriptorHandler() {
        return new DescriptorHandler();
    }

    protected PlatformMerger getPlatformMerger(Platform platform, DescriptorHandler handler) {
        return new PlatformMerger(platform, handler);
    }

    protected TargetMerger getTargetMerger(Target target, DescriptorHandler handler) {
        return new TargetMerger(target, handler);
    }

    protected Map<String, Object> getParameters(Module module) {
        return module.getProperties();
    }

    protected void setParameters(Module module, Map<String, Object> parameters) {
        module.setProperties(parameters);
    }

    protected DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }

    protected ConfigurationParser getConfigurationParser() {
        return new ConfigurationParser();
    }

    protected DescriptorMerger getDescriptorMerger() {
        return new DescriptorMerger();
    }

    protected DeployTargetFactory getDeployTargetFactory() {
        return new DeployTargetFactory();
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
                return servicesBuilder.build();
            }
        }, expected[1], getClass(), new TestUtil.JsonSerializationOptions(false, true));
    }

}
