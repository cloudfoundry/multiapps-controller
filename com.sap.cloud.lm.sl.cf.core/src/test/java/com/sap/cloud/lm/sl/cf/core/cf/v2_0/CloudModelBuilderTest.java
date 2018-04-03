package com.sap.cloud.lm.sl.cf.core.cf.v2_0;

import java.util.Arrays;
import java.util.Map;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.ApplicationsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.CloudModelConfiguration;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.DomainsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.ServicesCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.DeployTargetFactory;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.handlers.v2_0.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.handlers.v2_0.DescriptorMerger;
import com.sap.cloud.lm.sl.mta.handlers.v2_0.DescriptorParser;
import com.sap.cloud.lm.sl.mta.mergers.v2_0.PlatformMerger;
import com.sap.cloud.lm.sl.mta.mergers.v2_0.TargetMerger;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Platform;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;
import com.sap.cloud.lm.sl.mta.model.v2_0.Module;
import com.sap.cloud.lm.sl.mta.resolvers.ResolverBuilder;
import com.sap.cloud.lm.sl.mta.resolvers.v2_0.DescriptorReferenceResolver;

@RunWith(value = Parameterized.class)
public class CloudModelBuilderTest extends com.sap.cloud.lm.sl.cf.core.cf.v1_0.CloudModelBuilderTest {

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (00) Full MTA:
            { "/mta/javahelloworld/mtad-v2.yaml", "/mta/javahelloworld/config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                false, false,
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/javahelloworld/services.json",
                    "R:/mta/javahelloworld/apps-v2.json", } },
            // (01)
            { "/mta/javahelloworld/mtad-v2.yaml", "/mta/javahelloworld/xs2-config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                false, false,
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/javahelloworld/xs2-services.json",
                    "R:/mta/javahelloworld/xs2-apps-v2.json", } },
            // (02) Full MTA with namespaces:
            { "/mta/javahelloworld/mtad-v2.yaml", "/mta/javahelloworld/config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                true, true,
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/javahelloworld/services-ns.json",
                    "R:/mta/javahelloworld/apps-ns-v2.json", } },
            // (03) Full MTA with namespaces (w/o services):
            { "/mta/javahelloworld/mtad-v2.yaml", "/mta/javahelloworld/config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                true, false,
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/javahelloworld/services.json",
                    "R:/mta/javahelloworld/apps-ns2-v2.json", } },
            // (04) Patch MTA (resolved inter-module dependencies):
            { "/mta/javahelloworld/mtad-v2.yaml", "/mta/javahelloworld/config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                false, false,
                new String[] { "java-hello-world" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/javahelloworld/services-patch1.json",
                    "R:/mta/javahelloworld/apps-patch1.json", } },
            // (05) Patch MTA with namespaces (resolved inter-module dependencies):
            { "/mta/javahelloworld/mtad-v2.yaml", "/mta/javahelloworld/config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                true, true,
                new String[] { "java-hello-world" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/javahelloworld/services-patch1-ns.json",
                    "R:/mta/javahelloworld/apps-patch1-ns.json", } },
            // (06) Patch MTA (unresolved inter-module dependencies):
            { "/mta/javahelloworld/mtad-v2.yaml", "/mta/javahelloworld/config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                false, false,
                new String[] { "java-hello-world" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] { "java-hello-world", }, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/javahelloworld/services-patch1.json",
                    "E:Unresolved MTA modules [java-hello-world-backend, java-hello-world-db]", } },
            // (07) Patch MTA (module is in archive, but not intended for platform):
            { "/mta/javahelloworld/mtad-v2.yaml", "/mta/javahelloworld/config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                false, false,
                new String[] { "java-hello-world", }, // mtaArchiveModules
                new String[] {}, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                "[]", // domains
                "R:/mta/javahelloworld/services-patch1.json",
                "E:MTA module \"java-hello-world\" is part of MTA archive, but is not intended for deployment", } },
            // (08)
            { "/mta/shine/mtad-v2.yaml", "/mta/shine/config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                false, false,
                new String[] { "shine", "shine-xsjs", "shine-odata" }, // mtaArchiveModules
                new String[] { "shine", "shine-xsjs", "shine-odata" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/shine/services.json",
                    "R:/mta/shine/apps-v2.json", } },
            // (09)
            { "/mta/sample/mtad-v2.yaml", "/mta/sample/config1-v2.mtaext", "/mta/sample/platform-types-v2.json", "/mta/sample/targets-v2.json",
                false, false,
                new String[] { "pricing", "pricing-db", "web-server" }, // mtaArchiveModules
                new String[] { "pricing", "pricing-db", "web-server" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[\"bestprice.sap.com\"]", // domains
                    "R:/mta/sample/services.json",
                    "R:/mta/sample/apps-v2.json", } },
            // (10)
            { "/mta/devxwebide/mtad-v2.yaml", "/mta/devxwebide/config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                false, false,
                new String[] { "webide" }, // mtaArchiveModules
                new String[] { "webide" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/devxwebide/services.json",
                    "R:/mta/devxwebide/apps.json", } },
            // (11)
            { "/mta/devxwebide/mtad-v2.yaml", "/mta/devxwebide/xs2-config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                false, false,
                new String[] { "webide" }, // mtaArchiveModules
                new String[] { "webide" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/devxwebide/services.json",
                    "R:/mta/devxwebide/xs2-apps.json", } },
            // (12)
            { "/mta/devxdi/mtad-v2.yaml", "/mta/devxdi/config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                false, false,
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaArchiveModules
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/devxdi/services.json",
                    "R:/mta/devxdi/apps-v2.json", } },
            // (13)
            { "/mta/devxdi/mtad-v2.yaml", "/mta/devxdi/xs2-config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                false, false,
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaArchiveModules
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/devxdi/xs2-services.json",
                    "R:/mta/devxdi/xs2-apps-v2.json", } },
            // (14)
            { "/mta/devxwebide/mtad-v2.yaml", "/mta/devxwebide/xs2-config2-v2.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                false, false,
                new String[] { "webide" }, // mtaArchiveModules
                new String[] { "webide" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/devxwebide/services.json",
                    "R:/mta/devxwebide/xs2-apps.json", } },
            // (15) Unknown typed resource parameters:
            { "/mta/devxdi/mtad-v2.yaml", "/mta/devxdi/xs2-config2-v2.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                false, false,
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaArchiveModules
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] {
                    "[]", // domains
                    "R:/mta/devxdi/xs2-services.json",
                    "R:/mta/devxdi/xs2-apps-v2.json", } },
            // (16) Service binding parameters in requires dependency:
            { "mtad-01.yaml", "config-01.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                        false, false,
                        new String[] { "foo", }, // mtaArchiveModules
                        new String[] { "foo", }, // mtaModules
                        new String[] {}, // deployedApps
                        new String[] {
                            "[]", // domains
                            "[]",
                            "R:apps-01.json", } },
            // (17) Service binding parameters in requires dependency:
            { "mtad-02.yaml", "config-01.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                                false, false,
                                new String[] { "foo", }, // mtaArchiveModules
                                new String[] { "foo", }, // mtaModules
                                new String[] {}, // deployedApps
                                new String[] {
                                    "[]", // domains
                                    "[]",
                                    "E:Invalid type for key \"foo#bar#config\", expected \"Map\" but got \"String\"", } },
            // (18) Custom application names are used:
            {
                "mtad-03.yaml", "config-02.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                false, false,
                new String[] { "module-1", "module-2" }, // mtaArchiveModules
                new String[] { "module-1", "module-2" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "[]", "R:apps-02.json", }
            },
            // (19) Custom application names are used:
            {
                "mtad-03.yaml", "config-02.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                true, true,
                new String[] { "module-1", "module-2" }, // mtaArchiveModules
                new String[] { "module-1", "module-2" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "[]", "R:apps-03.json", }
            },
            // (20) Temporary URIs are used:
            {
                "mtad-05.yaml", "config-02.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                false, false,
                new String[] { "module-1", "module-2" }, // mtaArchiveModules
                new String[] { "module-1", "module-2" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "[]", "R:apps-05.json", }
            },
            // (21) Use list parameter:
            {
                "mtad-06.yaml", "config-02.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                false, false,
                new String[] { "framework" }, // mtaArchiveModules
                new String[] { "framework" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "[]", "R:apps-06.json", }
            },
            // (22) Use partial plugin:
            {
                "mtad-07.yaml", "config-02.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                false, false,
                new String[] { "framework" }, // mtaArchiveModules
                new String[] { "framework" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "[]", "R:apps-07.json", }
            },
            // (23) Overwrite service-name resource property in ext. descriptor:
            {
                "mtad-08.yaml", "config-03.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                false, false,
                new String[] { "module-1" }, // mtaArchiveModules
                new String[] { "module-1" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "R:services-03.json", "R:apps-08.json", }
            },
            // (24) Test support for one-off tasks:
            {
                "mtad-09.yaml", "config-03.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                false, false,
                new String[] { "module-1", "module-2", "module-3", "module-4" }, // mtaArchiveModules
                new String[] { "module-1", "module-2", "module-3", "module-4" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "[]", "R:apps-09.json", }
            },
            // (25) With 'health-check-type' set to 'port':
            { 
                "mtad-health-check-type-port.yaml", "config-03.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "[]", "R:apps-with-health-check-type-port.json", }
            },
            // (26) With 'health-check-type' set to 'http' and a non-default 'health-check-http-endpoint':
            { 
                "mtad-health-check-type-http-with-endpoint.yaml", "config-03.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "[]", "R:apps-with-health-check-type-http-with-endpoint.json", }
            },
            // (27) With 'health-check-type' set to 'http' and no 'health-check-http-endpoint':
            { 
                "mtad-health-check-type-http-without-endpoint.yaml", "config-03.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "[]", "R:apps-with-health-check-type-http-without-endpoint.json", }
            },
            // (28) Test inject service keys:
            {
                "mtad-10.yaml", "config-02.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                false, false,
                new String[] { "module-1" }, // mtaArchiveModules
                new String[] { "module-1" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "[]", "R:apps-10.json", }
            },
            // (29) With 'enable-ssh' set to true: 
            {
                "mtad-ssh-enabled-true.yaml", "config-02.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "[]", "R:apps-with-ssh-enabled-true.json", }
            },
            // (30) With 'enable-ssh' set to false: 
            {
                "mtad-ssh-enabled-false.yaml", "config-02.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[]", "[]", "R:apps-with-ssh-enabled-false.json", }
            },
            // (31) With TCPS routes
            {
                "mtad-11.yaml", "config-02.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json",
                false, false,
                new String[] { "module-1", "module-2", "module-3" }, // mtaArchiveModules
                new String[] { "module-1", "module-2", "module-3" }, // mtaModules
                new String[] {}, // deployedApps
                new String[] { "[\"test-domain\",\"test-domain-2\"]", "[]", "R:apps-with-tcp-routes.json", }
            }
// @formatter:on
        });
    }

    public CloudModelBuilderTest(String deploymentDescriptorLocation, String extensionDescriptorLocation, String platformsLocation,
        String targetsLocation, boolean useNamespaces, boolean useNamespacesForServices, String[] mtaArchiveModules, String[] mtaModules,
        String[] deployedApps, String[] expected) {
        super(deploymentDescriptorLocation, extensionDescriptorLocation, platformsLocation, targetsLocation, useNamespaces,
            useNamespacesForServices, mtaArchiveModules, mtaModules, deployedApps, expected);
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
    protected ServicesCloudModelBuilder getServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        CloudModelConfiguration configuration) {
        return new ServicesCloudModelBuilder((com.sap.cloud.lm.sl.mta.model.v2_0.DeploymentDescriptor) deploymentDescriptor,
            new HandlerFactory(2).getPropertiesAccessor(), configuration);
    }

    @Override
    protected ApplicationsCloudModelBuilder getApplicationsCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        SystemParameters systemParameters, XsPlaceholderResolver xsPlaceholderResolver, CloudModelConfiguration configuration) {
        deploymentDescriptor = new DescriptorReferenceResolver(
            (com.sap.cloud.lm.sl.mta.model.v2_0.DeploymentDescriptor) deploymentDescriptor, new ResolverBuilder(), new ResolverBuilder())
                .resolve();
        return new com.sap.cloud.lm.sl.cf.core.cf.v2_0.ApplicationsCloudModelBuilder(
            (com.sap.cloud.lm.sl.mta.model.v2_0.DeploymentDescriptor) deploymentDescriptor, configuration, null, systemParameters,
            xsPlaceholderResolver, DEPLOY_ID);
    }

    @Override
    protected DomainsCloudModelBuilder getDomainsBuilder(DeploymentDescriptor deploymentDescriptor, SystemParameters systemParameters,
        XsPlaceholderResolver xsPlaceholderResolver) {
        return new com.sap.cloud.lm.sl.cf.core.cf.v2_0.DomainsCloudModelBuilder(systemParameters, xsPlaceholderResolver,
            (com.sap.cloud.lm.sl.mta.model.v2_0.DeploymentDescriptor) deploymentDescriptor);
    }

    @Override
    protected TargetMerger getTargetMerger(Target target, DescriptorHandler handler) {
        return new com.sap.cloud.lm.sl.mta.mergers.v2_0.TargetMerger((com.sap.cloud.lm.sl.mta.model.v2_0.Target) target,
            (com.sap.cloud.lm.sl.mta.handlers.v2_0.DescriptorHandler) handler);
    }

    @Override
    protected PlatformMerger getPlatformMerger(Platform platform, DescriptorHandler handler) {
        return new com.sap.cloud.lm.sl.mta.mergers.v2_0.PlatformMerger((com.sap.cloud.lm.sl.mta.model.v2_0.Platform) platform,
            (com.sap.cloud.lm.sl.mta.handlers.v2_0.DescriptorHandler) handler);
    }

    @Override
    protected void setParameters(com.sap.cloud.lm.sl.mta.model.v1_0.Module module, Map<String, Object> parameters) {
        ((Module) module).setParameters(parameters);
    }

    @Override
    protected DeployTargetFactory getDeployTargetFactory() {
        return new DeployTargetFactory();
    }

    @Override
    protected DescriptorMerger getDescriptorMerger() {
        return new DescriptorMerger();
    }

}
