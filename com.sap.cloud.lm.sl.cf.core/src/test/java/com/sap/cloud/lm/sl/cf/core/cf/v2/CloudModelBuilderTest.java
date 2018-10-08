package com.sap.cloud.lm.sl.cf.core.cf.v2;

import java.util.Arrays;
import java.util.Map;

import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.v1.ApplicationsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v1.CloudModelConfiguration;
import com.sap.cloud.lm.sl.cf.core.cf.v1.DomainsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v1.ServicesCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.DeployTargetFactory;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;
import com.sap.cloud.lm.sl.mta.handlers.v1.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.handlers.v2.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorMerger;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorParser;
import com.sap.cloud.lm.sl.mta.mergers.v2.PlatformMerger;
import com.sap.cloud.lm.sl.mta.mergers.v2.TargetMerger;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1.Platform;
import com.sap.cloud.lm.sl.mta.model.v1.Target;
import com.sap.cloud.lm.sl.mta.model.v2.Module;
import com.sap.cloud.lm.sl.mta.resolvers.ResolverBuilder;
import com.sap.cloud.lm.sl.mta.resolvers.v2.DescriptorReferenceResolver;

public class CloudModelBuilderTest extends com.sap.cloud.lm.sl.cf.core.cf.v1.CloudModelBuilderTest {

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (00) Full MTA:
            { "/mta/javahelloworld/mtad-v2.yaml", "/mta/javahelloworld/config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                false, false,
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"), // domains
                    new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/services.json"),
                    new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/apps-v2.json"), } },
            // (01)
            { "/mta/javahelloworld/mtad-v2.yaml", "/mta/javahelloworld/xs2-config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                false, false,
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"), // domains
                    new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/xs2-services.json"),
                    new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/xs2-apps-v2.json"), } },
            // (02) Full MTA with namespaces:
            { "/mta/javahelloworld/mtad-v2.yaml", "/mta/javahelloworld/config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                true, true,
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"), // domains
                    new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/services-ns.json"),
                    new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/apps-ns-v2.json"), } },
            // (03) Full MTA with namespaces (w/o services):
            { "/mta/javahelloworld/mtad-v2.yaml", "/mta/javahelloworld/config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                true, false,
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"), // domains
                    new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/services.json"),
                    new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/apps-ns2-v2.json"), } },
            // (04) Patch MTA (resolved inter-module dependencies):
            { "/mta/javahelloworld/mtad-v2.yaml", "/mta/javahelloworld/config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                false, false,
                new String[] { "java-hello-world" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // deployedApps
                new Expectation[] {
                    new Expectation("[]"), // domains
                    new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/services-patch1.json"),
                    new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/apps-patch1.json"), } },
            // (05) Patch MTA with namespaces (resolved inter-module dependencies):
            { "/mta/javahelloworld/mtad-v2.yaml", "/mta/javahelloworld/config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                true, true,
                new String[] { "java-hello-world" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // deployedApps
                new Expectation[] {
                    new Expectation("[]"), // domains
                    new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/services-patch1-ns.json"),
                    new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/apps-patch1-ns.json"), } },
            // (06) Patch MTA (unresolved inter-module dependencies):
            { "/mta/javahelloworld/mtad-v2.yaml", "/mta/javahelloworld/config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                false, false,
                new String[] { "java-hello-world" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] { "java-hello-world", }, // deployedApps
                new Expectation[] {
                    new Expectation("[]"), // domains
                    new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/services-patch1.json"),
                    new Expectation(Expectation.Type.EXCEPTION, "Unresolved MTA modules [java-hello-world-db, java-hello-world-backend]") } },
            // (07)
            { "/mta/shine/mtad-v2.yaml", "/mta/shine/config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                false, false,
                new String[] { "shine", "shine-xsjs", "shine-odata" }, // mtaArchiveModules
                new String[] { "shine", "shine-xsjs", "shine-odata" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"), // domains
                    new Expectation(Expectation.Type.RESOURCE, "/mta/shine/services.json"),
                    new Expectation(Expectation.Type.RESOURCE, "/mta/shine/apps-v2.json"), } },
            // (08)
            { "/mta/sample/mtad-v2.yaml", "/mta/sample/config1-v2.mtaext", "/mta/sample/platform-types-v2.json", "/mta/sample/targets-v2.json", null,
                false, false,
                new String[] { "pricing", "pricing-db", "web-server" }, // mtaArchiveModules
                new String[] { "pricing", "pricing-db", "web-server" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[bestprice.sap.com]"), // domains
                    new Expectation(Expectation.Type.RESOURCE, "/mta/sample/services.json"),
                    new Expectation(Expectation.Type.RESOURCE, "/mta/sample/apps-v2.json"), } },
            // (09)
            { "/mta/devxwebide/mtad-v2.yaml", "/mta/devxwebide/config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                false, false,
                new String[] { "webide" }, // mtaArchiveModules
                new String[] { "webide" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"), // domains
                    new Expectation(Expectation.Type.RESOURCE, "/mta/devxwebide/services.json"),
                    new Expectation(Expectation.Type.RESOURCE, "/mta/devxwebide/apps.json"), } },
            // (10)
            { "/mta/devxwebide/mtad-v2.yaml", "/mta/devxwebide/xs2-config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                false, false,
                new String[] { "webide" }, // mtaArchiveModules
                new String[] { "webide" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"), // domains
                    new Expectation(Expectation.Type.RESOURCE, "/mta/devxwebide/services.json"),
                    new Expectation(Expectation.Type.RESOURCE, "/mta/devxwebide/xs2-apps.json"), } },
            // (11)
            { "/mta/devxdi/mtad-v2.yaml", "/mta/devxdi/config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                false, false,
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaArchiveModules
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"), // domains
                    new Expectation(Expectation.Type.RESOURCE, "/mta/devxdi/services.json"),
                    new Expectation(Expectation.Type.RESOURCE, "/mta/devxdi/apps-v2.json"), } },
            // (12)
            { "/mta/devxdi/mtad-v2.yaml", "/mta/devxdi/xs2-config1-v2.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                false, false,
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaArchiveModules
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"), // domains
                    new Expectation(Expectation.Type.RESOURCE, "/mta/devxdi/xs2-services.json"),
                    new Expectation(Expectation.Type.RESOURCE, "/mta/devxdi/xs2-apps-v2.json"), } },
            // (13)
            { "/mta/devxwebide/mtad-v2.yaml", "/mta/devxwebide/xs2-config2-v2.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                false, false,
                new String[] { "webide" }, // mtaArchiveModules
                new String[] { "webide" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"), // domains
                    new Expectation(Expectation.Type.RESOURCE, "/mta/devxwebide/services.json"),
                    new Expectation(Expectation.Type.RESOURCE, "/mta/devxwebide/xs2-apps.json"), } },
            // (14) Unknown typed resource parameters:
            { "/mta/devxdi/mtad-v2.yaml", "/mta/devxdi/xs2-config2-v2.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                false, false,
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaArchiveModules
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"), // domains
                    new Expectation(Expectation.Type.RESOURCE, "/mta/devxdi/xs2-services.json"),
                    new Expectation(Expectation.Type.RESOURCE, "/mta/devxdi/xs2-apps-v2.json"), } },
            // (15) Service binding parameters in requires dependency:
            { "mtad-01.yaml", "config-01.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                false, false,
                new String[] { "foo", }, // mtaArchiveModules
                new String[] { "foo", }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"), // domains
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "apps-01.json"), } },
            // (16) Service binding parameters in requires dependency:
            { "mtad-02.yaml", "config-01.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                false, false,
                new String[] { "foo", }, // mtaArchiveModules
                new String[] { "foo", }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"), // domains
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.EXCEPTION, "Invalid type for key \"foo#bar#config\", expected \"Map\" but got \"String\""), } },
            // (17) Custom application names are used:
            {
                "mtad-03.yaml", "config-02.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                false, false,
                new String[] { "module-1", "module-2" }, // mtaArchiveModules
                new String[] { "module-1", "module-2" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "apps-02.json"), }
            },
            // (18) Custom application names are used:
            {
                "mtad-03.yaml", "config-02.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                true, true,
                new String[] { "module-1", "module-2" }, // mtaArchiveModules
                new String[] { "module-1", "module-2" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "apps-03.json"), }
            },
            // (19) Temporary URIs are used:
            {
                "mtad-05.yaml", "config-02.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                false, false,
                new String[] { "module-1", "module-2" }, // mtaArchiveModules
                new String[] { "module-1", "module-2" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "apps-05.json"), }
            },
            // (20) Use list parameter:
            {
                "mtad-06.yaml", "config-02.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                false, false,
                new String[] { "framework" }, // mtaArchiveModules
                new String[] { "framework" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "apps-06.json"), }
            },
            // (21) Use partial plugin:
            {
                "mtad-07.yaml", "config-02.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                false, false,
                new String[] { "framework" }, // mtaArchiveModules
                new String[] { "framework" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "apps-07.json"), }
            },
            // (22) Overwrite service-name resource property in ext. descriptor:
            {
                "mtad-08.yaml", "config-03.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                false, false,
                new String[] { "module-1" }, // mtaArchiveModules
                new String[] { "module-1" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "services-03.json"),
                    new Expectation(Expectation.Type.RESOURCE, "apps-08.json"), }
            },
            // (23) Test support for one-off tasks:
            {
                "mtad-09.yaml", "config-03.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                false, false,
                new String[] { "module-1", "module-2", "module-3", "module-4" }, // mtaArchiveModules
                new String[] { "module-1", "module-2", "module-3", "module-4" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "apps-09.json"), }
            },
            // (24) With 'health-check-type' set to 'port':
            { 
                "mtad-health-check-type-port.yaml", "config-03.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "apps-with-health-check-type-port.json"), }
            },
            // (25) With 'health-check-type' set to 'http' and a non-default 'health-check-http-endpoint':
            { 
                "mtad-health-check-type-http-with-endpoint.yaml", "config-03.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "apps-with-health-check-type-http-with-endpoint.json"), }
            },
            // (26) With 'health-check-type' set to 'http' and no 'health-check-http-endpoint':
            { 
                "mtad-health-check-type-http-without-endpoint.yaml", "config-03.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "apps-with-health-check-type-http-without-endpoint.json"), }
            },
            // (27) Test inject service keys:
            {
                "mtad-10.yaml", "config-02.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                false, false,
                new String[] { "module-1" }, // mtaArchiveModules
                new String[] { "module-1" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "apps-10.json"), }
            },
            // (28) With 'enable-ssh' set to true: 
            {
                "mtad-ssh-enabled-true.yaml", "config-02.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "apps-with-ssh-enabled-true.json"), }
            },
            // (29) With 'enable-ssh' set to false: 
            {
                "mtad-ssh-enabled-false.yaml", "config-02.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "apps-with-ssh-enabled-false.json"), }
            },
            // (30) With TCPS routes
            {
                "mtad-11.yaml", "config-02.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                false, false,
                new String[] { "module-1", "module-2", "module-3" }, // mtaArchiveModules
                new String[] { "module-1", "module-2", "module-3" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[test-domain, test-domain-2]"),
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "apps-with-tcp-routes.json"), }
            },
            // (31) Shared Managed Service
            { "/mta/sample/mtad-v2-shared.yaml", "/mta/sample/config1-v2.mtaext", "/mta/sample/platform-types-v2.json", "/mta/sample/targets-v2.json", null,
                false, false,
                new String[] { "pricing", "pricing-db", "web-server" }, // mtaArchiveModules
                new String[] { "pricing", "pricing-db", "web-server" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[bestprice.sap.com]"), // domains
                    new Expectation(Expectation.Type.RESOURCE, "/mta/sample/services-shared.json"),
                    new Expectation(Expectation.Type.RESOURCE, "/mta/sample/apps-v2-shared.json"), } 
            },
            // (32) Do not restart on env change - bg-deploy
            { "mtad-restart-on-env-change.yaml", "config-02.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                false, false, 
                new String[] { "module-1", "module-2", "module-3" }, // mtaArchiveModules
                new String[] { "module-1", "module-2", "module-3" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "apps-with-restart-parameters-false.json") // services
                }
            },
            // (34) With 'keep-existing-routes' set to true and no deployed MTA:
            {
                "keep-existing-routes/mtad.yaml", "config-02.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", null,
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "keep-existing-routes/apps.json"), }
            },
            // (35) With 'keep-existing-routes' set to true and no deployed module:
            {
                "keep-existing-routes/mtad.yaml", "config-02.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", 
                "keep-existing-routes/deployed-mta-without-foo-module.json",
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "keep-existing-routes/apps.json"), }
            },
            // (36) With 'keep-existing-routes' set to true and an already deployed module with no URIs:
            {
                "keep-existing-routes/mtad.yaml", "config-02.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", 
                "keep-existing-routes/deployed-mta-without-uris.json",
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "keep-existing-routes/apps.json"), }
            },
            // (37) With 'keep-existing-routes' set to true and an already deployed module:
            {
                "keep-existing-routes/mtad.yaml", "config-02.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", 
                "keep-existing-routes/deployed-mta.json",
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "keep-existing-routes/apps-with-existing-routes.json"), }
            },
            // (38) With global 'keep-existing-routes' set to true and an already deployed module:
            {
                "keep-existing-routes/mtad-with-global-parameter.yaml", "config-02.mtaext", "/mta/platform-types-v2.json", "/mta/targets-v2.json", 
                "keep-existing-routes/deployed-mta.json",
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "keep-existing-routes/apps-with-existing-routes.json"), }
            },
// @formatter:on
        });
    }

    public CloudModelBuilderTest(String deploymentDescriptorLocation, String extensionDescriptorLocation, String platformsLocation,
        String targetsLocation, String deployedMtaLocation, boolean useNamespaces, boolean useNamespacesForServices,
        String[] mtaArchiveModules, String[] mtaModules, String[] deployedApps, Expectation[] expectations) {
        super(deploymentDescriptorLocation, extensionDescriptorLocation, platformsLocation, targetsLocation, deployedMtaLocation,
            useNamespaces, useNamespacesForServices, mtaArchiveModules, mtaModules, deployedApps, expectations);
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
    protected Map<String, Object> getParameters(com.sap.cloud.lm.sl.mta.model.v1.Module module) {
        return ((Module) module).getParameters();
    }

    @Override
    protected com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorHandler getDescriptorHandler() {
        return new com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorHandler();
    }

    @Override
    protected ServicesCloudModelBuilder getServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        CloudModelConfiguration configuration) {
        return new ServicesCloudModelBuilder((com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor) deploymentDescriptor,
            new HandlerFactory(2).getPropertiesAccessor(), configuration);
    }

    @Override
    protected ApplicationsCloudModelBuilder getApplicationsCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        CloudModelConfiguration configuration, DeployedMta deployedMta, SystemParameters systemParameters,
        XsPlaceholderResolver xsPlaceholderResolver) {
        deploymentDescriptor = new DescriptorReferenceResolver((com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor) deploymentDescriptor,
            new ResolverBuilder(), new ResolverBuilder()).resolve();
        return new com.sap.cloud.lm.sl.cf.core.cf.v2.ApplicationsCloudModelBuilder(
            (com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor) deploymentDescriptor, configuration, deployedMta, systemParameters,
            xsPlaceholderResolver, DEPLOY_ID);
    }

    @Override
    protected DomainsCloudModelBuilder getDomainsBuilder(DeploymentDescriptor deploymentDescriptor, SystemParameters systemParameters,
        XsPlaceholderResolver xsPlaceholderResolver) {
        return new com.sap.cloud.lm.sl.cf.core.cf.v2.DomainsCloudModelBuilder(systemParameters, xsPlaceholderResolver,
            (com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor) deploymentDescriptor);
    }

    @Override
    protected TargetMerger getTargetMerger(Target target, DescriptorHandler handler) {
        return new com.sap.cloud.lm.sl.mta.mergers.v2.TargetMerger((com.sap.cloud.lm.sl.mta.model.v2.Target) target,
            (com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorHandler) handler);
    }

    @Override
    protected PlatformMerger getPlatformMerger(Platform platform, DescriptorHandler handler) {
        return new com.sap.cloud.lm.sl.mta.mergers.v2.PlatformMerger((com.sap.cloud.lm.sl.mta.model.v2.Platform) platform,
            (com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorHandler) handler);
    }

    @Override
    protected void setParameters(com.sap.cloud.lm.sl.mta.model.v1.Module module, Map<String, Object> parameters) {
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
