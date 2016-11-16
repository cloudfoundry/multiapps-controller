package com.sap.cloud.lm.sl.cf.core.cf.v2_0;

import java.util.Arrays;
import java.util.Map;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.TargetPlatformFactory;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.handlers.v2_0.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.handlers.v2_0.DescriptorMerger;
import com.sap.cloud.lm.sl.mta.handlers.v2_0.DescriptorParser;
import com.sap.cloud.lm.sl.mta.mergers.v2_0.TargetPlatformMerger;
import com.sap.cloud.lm.sl.mta.mergers.v2_0.TargetPlatformTypeMerger;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatformType;
import com.sap.cloud.lm.sl.mta.model.v2_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2_0.Module;
import com.sap.cloud.lm.sl.mta.resolvers.v2_0.DescriptorReferenceResolver;

@RunWith(value = Parameterized.class)
public class CloudModelBuilderTest extends com.sap.cloud.lm.sl.cf.core.cf.v1_0.CloudModelBuilderTest {

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (00) Full MTA:
            { "/mta/javahelloworld/mtad-v2.yaml", "/mta/javahelloworld/config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/platforms-v2.json",
                false, false, true,
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/javahelloworld/services.json",
                    "R:/mta/javahelloworld/apps-v2.json", } },
            // (01)
            { "/mta/javahelloworld/mtad-v2.yaml", "/mta/javahelloworld/xs2-config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/platforms-v2.json",
                false, false, true,
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/javahelloworld/xs2-services.json",
                    "R:/mta/javahelloworld/xs2-apps-v2.json", } },
            // (02) Full MTA with namespaces:
            { "/mta/javahelloworld/mtad-v2.yaml", "/mta/javahelloworld/config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/platforms-v2.json",
                true, true, true,
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/javahelloworld/services-ns.json",
                    "R:/mta/javahelloworld/apps-ns-v2.json", } },
            // (03) Full MTA with namespaces (w/o services):
            { "/mta/javahelloworld/mtad-v2.yaml", "/mta/javahelloworld/config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/platforms-v2.json",
                true, false, true,
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/javahelloworld/services.json",
                    "R:/mta/javahelloworld/apps-ns2-v2.json", } },
            // (04) Patch MTA (resolved inter-module dependencies):
            { "/mta/javahelloworld/mtad-v2.yaml", "/mta/javahelloworld/config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/platforms-v2.json",
                false, false, true,
                new String[] { "java-hello-world" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/javahelloworld/services-patch1.json",
                    "R:/mta/javahelloworld/apps-patch1.json", } },
            // (05) Patch MTA with namespaces (resolved inter-module dependencies):
            { "/mta/javahelloworld/mtad-v2.yaml", "/mta/javahelloworld/config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/platforms-v2.json",
                true, true, true,
                new String[] { "java-hello-world" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/javahelloworld/services-patch1-ns.json",
                    "R:/mta/javahelloworld/apps-patch1-ns.json", } },
            // (06) Patch MTA (unresolved inter-module dependencies):
            { "/mta/javahelloworld/mtad-v2.yaml", "/mta/javahelloworld/config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/platforms-v2.json",
                false, false, true,
                new String[] { "java-hello-world" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] { "java-hello-world", }, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/javahelloworld/services-patch1.json",
                    "E:Unresolved MTA modules [java-hello-world-backend, java-hello-world-db]", } },
            // (07) Patch MTA (module is in archive, but not intended for platform):
            { "/mta/javahelloworld/mtad-v2.yaml", "/mta/javahelloworld/config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/platforms-v2.json",
                false, false, true,
                new String[] { "java-hello-world", }, // mtaArchiveModules
                new String[] {}, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                "[]", // domains
                "R:/mta/javahelloworld/services-patch1.json",
                "E:MTA module \"java-hello-world\" is part of MTA archive, but is not intended for deployment", } },
            // (08)
            { "/mta/shine/mtad-v2.yaml", "/mta/shine/config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/platforms-v2.json",
                false, false, true,
                new String[] { "shine", "shine-xsjs", "shine-odata" }, // mtaArchiveModules
                new String[] { "shine", "shine-xsjs", "shine-odata" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/shine/services.json",
                    "R:/mta/shine/apps-v2.json", } },
            // (09)
            { "/mta/sample/mtad-v2.yaml", "/mta/sample/config1-v2.mtaext", "/mta/sample/platform-types-v2.json", "/mta/sample/platforms-v2.json",
                false, false, true,
                new String[] { "pricing", "pricing-db", "web-server" }, // mtaArchiveModules
                new String[] { "pricing", "pricing-db", "web-server" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[\"bestprice.sap.com\"]", // domains
                    "R:/mta/sample/services.json",
                    "R:/mta/sample/apps-v2.json", } },
            // (10)
            { "/mta/sample/mtad-v2.yaml", "/mta/sample/config1-v2.mtaext", "/mta/sample/platform-types-v2.json", "/mta/sample/platforms-v2.json",
                false, false, false,
                new String[] { "pricing", "pricing-db", "web-server" }, // mtaArchiveModules
                new String[] { "pricing", "pricing-db", "web-server" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[\"bestprice.sap.com\"]", // domains
                    "R:/mta/sample/services.json",
                    "E:The name \"default-locale\" is not a valid environment variable name", } },
            // (11)
            { "/mta/devxwebide/mtad-v2.yaml", "/mta/devxwebide/config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/platforms-v2.json",
                false, false, true,
                new String[] { "webide" }, // mtaArchiveModules
                new String[] { "webide" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "[]",
                    "R:/mta/devxwebide/apps.json", } },
            // (12)
            { "/mta/devxwebide/mtad-v2.yaml", "/mta/devxwebide/xs2-config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/platforms-v2.json",
                false, false, true,
                new String[] { "webide" }, // mtaArchiveModules
                new String[] { "webide" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "[]",
                    "R:/mta/devxwebide/xs2-apps.json", } },
            // (13)
            { "/mta/devxdi/mtad-v2.yaml", "/mta/devxdi/config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/platforms-v2.json",
                false, false, true,
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaArchiveModules
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/devxdi/services.json",
                    "R:/mta/devxdi/apps-v2.json", } },
            // (14)
            { "/mta/devxdi/mtad-v2.yaml", "/mta/devxdi/xs2-config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/platforms-v2.json",
                false, false, true,
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaArchiveModules
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/devxdi/xs2-services.json",
                    "R:/mta/devxdi/xs2-apps-v2.json", } },
            // (15)
            { "/mta/devxwebide/mtad-v2.yaml", "/mta/devxwebide/xs2-config2-v2.mtaext", "/mta/platform-types-v2.json", "/mta/platforms-v2.json",
                false, false, true,
                new String[] { "webide" }, // mtaArchiveModules
                new String[] { "webide" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "[]",
                    "R:/mta/devxwebide/xs2-apps.json", } },
            // (16) Unknown typed resource parameters:
            { "/mta/devxdi/mtad-v2.yaml", "/mta/devxdi/xs2-config2-v2.mtaext", "/mta/platform-types-v2.json", "/mta/platforms-v2.json",
                false, false, true,
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaArchiveModules
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/devxdi/xs2-services.json",
                    "R:/mta/devxdi/xs2-apps-v2.json", } },
            // (17) Service binding parameters in requires dependency:
            { "mtad-01.yaml", "config-01.mtaext", "/mta/platform-types-v2.json", "/mta/platforms-v2.json",
                        false, false, true,
                        new String[] { "foo", }, // mtaArchiveModules
                        new String[] { "foo", }, // mtaModules
                        new String[] {}, // deployedApps
                        new String[] {
                            "[]", // domains
                            "R:services-01.json",
                            "R:apps-01.json", } },
            // (18) Service binding parameters in requires dependency:
            { "mtad-02.yaml", "config-01.mtaext", "/mta/platform-types-v2.json", "/mta/platforms-v2.json",
                                false, false, true,
                                new String[] { "foo", }, // mtaArchiveModules
                                new String[] { "foo", }, // mtaModules
                                new String[] {}, // deployedApps
                                new String[] {
                                    "[]", // domains
                                    "R:services-01.json",
                                    "E:Invalid type for key \"foo#bar#config\", expected \"Map\" but got \"String\"", } },
            // (19) Custom application names are used:
            {
                "mtad-03.yaml", "config-02.mtaext", "/mta/platform-types-v2.json", "/mta/platforms-v2.json",
                false, false, true,
                new String[] { "module-1", "module-2" }, // mtaArchiveModules
                new String[] { "module-1", "module-2" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "[]", "R:apps-02.json", }
            },
            // (20) Custom application names are used:
            {
                "mtad-03.yaml", "config-02.mtaext", "/mta/platform-types-v2.json", "/mta/platforms-v2.json",
                true, true, true,
                new String[] { "module-1", "module-2" }, // mtaArchiveModules
                new String[] { "module-1", "module-2" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "[]", "R:apps-03.json", }
            },
            // (21) Temporary URIs are used:
            {
                "mtad-05.yaml", "config-02.mtaext", "/mta/platform-types-v2.json", "/mta/platforms-v2.json",
                false, false, true,
                new String[] { "module-1", "module-2" }, // mtaArchiveModules
                new String[] { "module-1", "module-2" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "[]", "R:apps-05.json", }
            },
            // (22) Use list parameter:
            {
                "mtad-06.yaml", "config-02.mtaext", "/mta/platform-types-v2.json", "/mta/platforms-v2.json",
                false, false, true,
                new String[] { "framework" }, // mtaArchiveModules
                new String[] { "framework" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "[]", "R:apps-06.json", }
            },
            // (23) Use partial plugin:
            {
                "mtad-07.yaml", "config-02.mtaext", "/mta/platform-types-v2.json", "/mta/platforms-v2.json",
                false, false, true,
                new String[] { "framework" }, // mtaArchiveModules
                new String[] { "framework" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "[]", "R:apps-07.json", }
            },
            // (24) Overwrite service-name resource property in ext. descriptor:
            {
                "mtad-08.yaml", "config-03.mtaext", "/mta/platform-types-v2.json", "/mta/platforms-v2.json",
                false, false, true,
                new String[] { "module-1" }, // mtaArchiveModules
                new String[] { "module-1" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "R:services-03.json", "R:apps-08.json", }
            },
// @formatter:on
        });
    }

    public CloudModelBuilderTest(String deploymentDescriptorLocation, String extensionDescriptorLocation, String platformTypesLocation,
        String platformsLocation, boolean useNamespaces, boolean useNamespacesForServices, boolean allowInvalidEnvNames,
        String[] mtaArchiveModules, String[] mtaModules, String[] deployedApps, String[] expected) {
        super(deploymentDescriptorLocation, extensionDescriptorLocation, platformTypesLocation, platformsLocation, useNamespaces,
            useNamespacesForServices, allowInvalidEnvNames, mtaArchiveModules, mtaModules, deployedApps, expected);
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
    protected Map<String, Object> getParameters(com.sap.cloud.lm.sl.mta.model.v1_0.Module module) {
        return ((Module) module).getParameters();
    }

    @Override
    protected com.sap.cloud.lm.sl.mta.handlers.v2_0.DescriptorHandler getDescriptorHandler() {
        return new com.sap.cloud.lm.sl.mta.handlers.v2_0.DescriptorHandler();
    }

    @Override
    protected CloudModelBuilder createCloudModelBuilder(com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor deploymentDescriptor,
        TargetPlatformType platformType, TargetPlatform platform, SystemParameters systemParameters, String platformName,
        String defaultDomain) throws Exception {
        deploymentDescriptor = new DescriptorReferenceResolver((DeploymentDescriptor) deploymentDescriptor).resolve();
        XsPlaceholderResolver xsPlaceholderResolver = new XsPlaceholderResolver();
        xsPlaceholderResolver.setDefaultDomain(defaultDomain);
        return new CloudModelBuilder((DeploymentDescriptor) deploymentDescriptor, systemParameters, !platformName.startsWith("CF"), false,
            useNamespaces, useNamespacesForServices, allowInvalidEnvNames, DEPLOY_ID, xsPlaceholderResolver);
    }

    @Override
    protected TargetPlatformMerger getTargetPlatformMerger(TargetPlatform platform, DescriptorHandler handler) {
        return new com.sap.cloud.lm.sl.mta.mergers.v2_0.TargetPlatformMerger((com.sap.cloud.lm.sl.mta.model.v2_0.TargetPlatform) platform,
            (com.sap.cloud.lm.sl.mta.handlers.v2_0.DescriptorHandler) handler);
    }

    @Override
    protected TargetPlatformTypeMerger getTargetPlatformTypeMerger(TargetPlatformType platformType, DescriptorHandler handler) {
        return new com.sap.cloud.lm.sl.mta.mergers.v2_0.TargetPlatformTypeMerger(
            (com.sap.cloud.lm.sl.mta.model.v2_0.TargetPlatformType) platformType,
            (com.sap.cloud.lm.sl.mta.handlers.v2_0.DescriptorHandler) handler);
    }

    @Override
    protected void setParameters(com.sap.cloud.lm.sl.mta.model.v1_0.Module module, Map<String, Object> parameters) {
        ((Module) module).setParameters(parameters);
    }

    @Override
    protected TargetPlatformFactory getTargetPlatformFactory() {
        return new TargetPlatformFactory();
    }

    @Override
    protected DescriptorMerger getDescriptorMerger() {
        return new DescriptorMerger();
    }

}
