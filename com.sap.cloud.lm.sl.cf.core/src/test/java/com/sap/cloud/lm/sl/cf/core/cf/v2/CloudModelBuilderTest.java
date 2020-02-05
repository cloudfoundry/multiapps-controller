package com.sap.cloud.lm.sl.cf.core.cf.v2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.util.ModulesCloudModelBuilderContentCalculator;
import com.sap.cloud.lm.sl.cf.core.cf.util.ResourcesCloudModelBuilderContentCalculator;
import com.sap.cloud.lm.sl.cf.core.cf.util.UnresolvedModulesContentValidator;
import com.sap.cloud.lm.sl.cf.core.helpers.ModuleToDeployHelper;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.common.util.Callable;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.common.util.Tester;
import com.sap.cloud.lm.sl.common.util.Tester.Expectation;
import com.sap.cloud.lm.sl.mta.handlers.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorMerger;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorParser;
import com.sap.cloud.lm.sl.mta.mergers.PlatformMerger;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.ExtensionDescriptor;
import com.sap.cloud.lm.sl.mta.model.Module;
import com.sap.cloud.lm.sl.mta.model.Platform;
import com.sap.cloud.lm.sl.mta.model.Resource;
import com.sap.cloud.lm.sl.mta.resolvers.ResolverBuilder;
import com.sap.cloud.lm.sl.mta.resolvers.v2.DescriptorReferenceResolver;

@RunWith(Parameterized.class)
public class CloudModelBuilderTest {

    protected static final String DEFAULT_DOMAIN_CF = "cfapps.neo.ondemand.com";
    protected static final String DEFAULT_DOMAIN_XS = "sofd60245639a";

    protected static final String DEPLOY_ID = "123";

    protected final Tester tester = Tester.forClass(getClass());
    protected final DescriptorParser descriptorParser = getDescriptorParser();
    protected final ConfigurationParser configurationParser = new ConfigurationParser();
    protected final DescriptorHandler descriptorHandler = getDescriptorHandler();
    protected DeploymentDescriptor deploymentDescriptor;

    protected final String deploymentDescriptorLocation;
    protected final String extensionDescriptorLocation;
    protected final String platformLocation;
    protected final String deployedMtaLocation;
    protected final boolean useNamespaces;
    protected final boolean useNamespacesForServices;
    protected final Set<String> mtaArchiveModules;
    protected final Set<String> mtaModules;
    protected final Set<String> deployedApps;
    protected final Expectation expectedServices;
    protected final Expectation expectedApps;
    private ModulesCloudModelBuilderContentCalculator modulesCalculator;
    protected ModuleToDeployHelper moduleToDeployHelper;
    protected ResourcesCloudModelBuilderContentCalculator resourcesCalculator;

    protected ApplicationCloudModelBuilder appBuilder;
    protected ServicesCloudModelBuilder servicesBuilder;

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (00) Full MTA:
            { "/mta/javahelloworld/mtad.yaml", "/mta/javahelloworld/config.mtaext", "/mta/cf-platform.json", null,
                false, false,
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation(Expectation.Type.JSON, "/mta/javahelloworld/services.json"),
                new Expectation(Expectation.Type.JSON, "/mta/javahelloworld/apps.json"),
            },
            // (01)
            { "/mta/javahelloworld/mtad.yaml", "/mta/javahelloworld/xs2-config.mtaext", "/mta/xs-platform.json", null,
                false, false,
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation(Expectation.Type.JSON, "/mta/javahelloworld/xs2-services.json"),
                new Expectation(Expectation.Type.JSON, "/mta/javahelloworld/xs2-apps.json"),
            },
            // (02) Full MTA with namespaces:
            { "/mta/javahelloworld/mtad.yaml", "/mta/javahelloworld/config.mtaext", "/mta/cf-platform.json", null,
                true, true,
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation(Expectation.Type.JSON, "/mta/javahelloworld/services-ns.json"),
                new Expectation(Expectation.Type.JSON, "/mta/javahelloworld/apps-ns-1.json"),
            },
            // (03) Full MTA with namespaces (w/o services):
            { "/mta/javahelloworld/mtad.yaml", "/mta/javahelloworld/config.mtaext", "/mta/cf-platform.json", null,
                true, false,
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation(Expectation.Type.JSON, "/mta/javahelloworld/services-ns-2.json"),
                new Expectation(Expectation.Type.JSON, "/mta/javahelloworld/apps-ns-2.json"),
            },
            // (04) Patch MTA (resolved inter-module dependencies):
            { "/mta/javahelloworld/mtad.yaml", "/mta/javahelloworld/config.mtaext", "/mta/cf-platform.json", null,
                false, false,
                new String[] { "java-hello-world" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // deployedApps
                new Expectation(Expectation.Type.JSON, "/mta/javahelloworld/services-patch.json"),
                new Expectation(Expectation.Type.JSON, "/mta/javahelloworld/apps-patch.json"),
            },
            // (05) Patch MTA with namespaces (resolved inter-module dependencies):
            { "/mta/javahelloworld/mtad.yaml", "/mta/javahelloworld/config.mtaext", "/mta/cf-platform.json", null,
                true, true,
                new String[] { "java-hello-world" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // deployedApps
                new Expectation(Expectation.Type.JSON, "/mta/javahelloworld/services-patch-ns.json"),
                new Expectation(Expectation.Type.JSON, "/mta/javahelloworld/apps-patch-ns.json"),
            },
            // (06) Patch MTA (unresolved inter-module dependencies):
            { "/mta/javahelloworld/mtad.yaml", "/mta/javahelloworld/config.mtaext", "/mta/cf-platform.json", null,
                false, false,
                new String[] { "java-hello-world" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] { "java-hello-world", }, // deployedApps
                new Expectation(Expectation.Type.JSON, "/mta/javahelloworld/services-patch.json"),
                new Expectation(Expectation.Type.EXCEPTION, "Unresolved MTA modules [java-hello-world-backend, java-hello-world-db]")
            },
            // (07)
            { "/mta/shine/mtad.yaml", "/mta/shine/config.mtaext", "/mta/cf-platform.json", null,
                false, false,
                new String[] { "shine", "shine-xsjs", "shine-odata" }, // mtaArchiveModules
                new String[] { "shine", "shine-xsjs", "shine-odata" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation(Expectation.Type.JSON, "/mta/shine/services.json"),
                new Expectation(Expectation.Type.JSON, "/mta/shine/apps.json"),
            },
            // (08)
            { "/mta/sample/mtad.yaml", "/mta/sample/config.mtaext", "/mta/sample/platform.json", null,
                false, false,
                new String[] { "pricing", "pricing-db", "web-server" }, // mtaArchiveModules
                new String[] { "pricing", "pricing-db", "web-server" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation(Expectation.Type.JSON, "/mta/sample/services.json"),
                new Expectation(Expectation.Type.JSON, "/mta/sample/apps.json"),
            },
            // (09)
            { "/mta/devxwebide/mtad.yaml", "/mta/devxwebide/config.mtaext", "/mta/cf-platform.json", null,
                false, false,
                new String[] { "webide" }, // mtaArchiveModules
                new String[] { "webide" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation(Expectation.Type.JSON, "/mta/devxwebide/services.json"),
                new Expectation(Expectation.Type.JSON, "/mta/devxwebide/apps.json"),
            },
            // (10)
            { "/mta/devxwebide/mtad.yaml", "/mta/devxwebide/xs2-config-1.mtaext", "/mta/xs-platform.json", null,
                false, false,
                new String[] { "webide" }, // mtaArchiveModules
                new String[] { "webide" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation(Expectation.Type.JSON, "/mta/devxwebide/services.json"),
                new Expectation(Expectation.Type.JSON, "/mta/devxwebide/xs2-apps.json"),
            },
            // (11)
            { "/mta/devxdi/mtad.yaml", "/mta/devxdi/config.mtaext", "/mta/cf-platform.json", null,
                false, false,
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaArchiveModules
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation(Expectation.Type.JSON, "/mta/devxdi/services.json"),
                new Expectation(Expectation.Type.JSON, "/mta/devxdi/apps.json"),
            },
            // (12)
            { "/mta/devxdi/mtad.yaml", "/mta/devxdi/xs2-config-1.mtaext", "/mta/xs-platform.json", null,
                false, false,
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaArchiveModules
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation(Expectation.Type.JSON, "/mta/devxdi/xs2-services.json"),
                new Expectation(Expectation.Type.JSON, "/mta/devxdi/xs2-apps.json"),
            },
            // (13)
            { "/mta/devxwebide/mtad.yaml", "/mta/devxwebide/xs2-config-2.mtaext", "/mta/xs-platform.json", null,
                false, false,
                new String[] { "webide" }, // mtaArchiveModules
                new String[] { "webide" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation(Expectation.Type.JSON, "/mta/devxwebide/services.json"),
                new Expectation(Expectation.Type.JSON, "/mta/devxwebide/xs2-apps.json"), 
            },
            // (14) Unknown typed resource parameters:
            { "/mta/devxdi/mtad.yaml", "/mta/devxdi/xs2-config-2.mtaext", "/mta/xs-platform.json", null,
                false, false,
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaArchiveModules
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation(Expectation.Type.JSON, "/mta/devxdi/xs2-services.json"),
                new Expectation(Expectation.Type.JSON, "/mta/devxdi/xs2-apps.json"),
            },
            // (15) Service binding parameters in requires dependency:
            { "mtad-01.yaml", "config-01.mtaext", "/mta/cf-platform.json", null,
                false, false,
                new String[] { "foo", }, // mtaArchiveModules
                new String[] { "foo", }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.JSON, "apps-01.json"),
            },
            // (16) Service binding parameters in requires dependency:
            { "mtad-02.yaml", "config-01.mtaext", "/mta/cf-platform.json", null,
                false, false,
                new String[] { "foo", }, // mtaArchiveModules
                new String[] { "foo", }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.EXCEPTION, "Invalid type for key \"foo#bar#config\", expected \"Map\" but got \"String\""),
            },
            // (17) Custom application names are used:
            {
                "mtad-03.yaml", "config-02.mtaext", "/mta/xs-platform.json", null,
                false, false,
                new String[] { "module-1", "module-2" }, // mtaArchiveModules
                new String[] { "module-1", "module-2" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.JSON, "apps-02.json"),
            },
            // (18) Custom application names are used:
            {
                "mtad-03.yaml", "config-02.mtaext", "/mta/xs-platform.json", null,
                true, true,
                new String[] { "module-1", "module-2" }, // mtaArchiveModules
                new String[] { "module-1", "module-2" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.JSON, "apps-03.json"),
            },
            // (19) Temporary URIs are used:
            {
                "mtad-05.yaml", "config-02.mtaext", "/mta/xs-platform.json", null,
                false, false,
                new String[] { "module-1", "module-2" }, // mtaArchiveModules
                new String[] { "module-1", "module-2" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.JSON, "apps-05.json"),
            },
            // (20) Use list parameter:
            {
                "mtad-06.yaml", "config-02.mtaext", "/mta/xs-platform.json", null,
                false, false,
                new String[] { "framework" }, // mtaArchiveModules
                new String[] { "framework" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.JSON, "apps-06.json"),
            },
            // (21) Use partial plugin:
            {
                "mtad-07.yaml", "config-02.mtaext", "/mta/xs-platform.json", null,
                false, false,
                new String[] { "framework" }, // mtaArchiveModules
                new String[] { "framework" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.JSON, "apps-07.json"),
            },
            // (22) Overwrite service-name resource property in ext. descriptor:
            {
                "mtad-08.yaml", "config-03.mtaext", "/mta/xs-platform.json", null,
                false, false,
                new String[] { "module-1" }, // mtaArchiveModules
                new String[] { "module-1" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation(Expectation.Type.JSON, "services-03.json"),
                new Expectation(Expectation.Type.JSON, "apps-08.json"),
            },
            // (23) Test support for one-off tasks:
            {
                "mtad-09.yaml", "config-03.mtaext", "/mta/xs-platform.json", null,
                false, false,
                new String[] { "module-1", "module-2", "module-3", "module-4" }, // mtaArchiveModules
                new String[] { "module-1", "module-2", "module-3", "module-4" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.JSON, "apps-09.json"),
            },
            // (24) With 'health-check-type' set to 'port':
            { 
                "mtad-health-check-type-port.yaml", "config-03.mtaext", "/mta/xs-platform.json", null,
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.JSON, "apps-with-health-check-type-port.json"),
            },
            // (25) With 'health-check-type' set to 'http' and a non-default 'health-check-http-endpoint':
            { 
                "mtad-health-check-type-http-with-endpoint.yaml", "config-03.mtaext", "/mta/xs-platform.json", null,
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.JSON, "apps-with-health-check-type-http-with-endpoint.json"),
            },
            // (26) With 'health-check-type' set to 'http' and no 'health-check-http-endpoint':
            { 
                "mtad-health-check-type-http-without-endpoint.yaml", "config-03.mtaext", "/mta/xs-platform.json", null,
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.JSON, "apps-with-health-check-type-http-without-endpoint.json"),
            },
            // (27) Test inject service keys:
            {
                "mtad-10.yaml", "config-02.mtaext", "/mta/xs-platform.json", null,
                false, false,
                new String[] { "module-1" }, // mtaArchiveModules
                new String[] { "module-1" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.JSON, "apps-10.json"),
            },
            // (28) With 'enable-ssh' set to true: 
            {
                "mtad-ssh-enabled-true.yaml", "config-02.mtaext", "/mta/xs-platform.json", null,
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.JSON, "apps-with-ssh-enabled-true.json"),
            },
            // (29) With 'enable-ssh' set to false: 
            {
                "mtad-ssh-enabled-false.yaml", "config-02.mtaext", "/mta/xs-platform.json", null,
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.JSON, "apps-with-ssh-enabled-false.json"),
            },
            // (30) Do not restart on env change - bg-deploy
            { "mtad-restart-on-env-change.yaml", "config-02.mtaext", "/mta/xs-platform.json", null,
                false, false, 
                new String[] { "module-1", "module-2", "module-3" }, // mtaArchiveModules
                new String[] { "module-1", "module-2", "module-3" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.JSON, "apps-with-restart-parameters-false.json") // services
            },
            // (31) With 'keep-existing-routes' set to true and no deployed MTA:
            {
                "keep-existing-routes/mtad.yaml", "config-02.mtaext", "/mta/xs-platform.json", null,
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.JSON, "keep-existing-routes/apps.json"),
            },
            // (32) With 'keep-existing-routes' set to true and no deployed module:
            {
                "keep-existing-routes/mtad.yaml", "config-02.mtaext", "/mta/xs-platform.json", 
                "keep-existing-routes/deployed-mta-without-foo-module.json",
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.JSON, "keep-existing-routes/apps.json"),
            },
            // (33) With 'keep-existing-routes' set to true and an already deployed module with no URIs:
            {
                "keep-existing-routes/mtad.yaml", "config-02.mtaext", "/mta/xs-platform.json", 
                "keep-existing-routes/deployed-mta-without-uris.json",
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.JSON, "keep-existing-routes/apps.json"),
            },
            // (34) With 'keep-existing-routes' set to true and an already deployed module:
            {
                "keep-existing-routes/mtad.yaml", "config-02.mtaext", "/mta/xs-platform.json", 
                "keep-existing-routes/deployed-mta.json",
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.JSON, "keep-existing-routes/apps-with-existing-routes.json"),
            },
            // (35) With global 'keep-existing-routes' set to true and an already deployed module:
            {
                "keep-existing-routes/mtad-with-global-parameter.yaml", "config-02.mtaext", "/mta/xs-platform.json", 
                "keep-existing-routes/deployed-mta.json",
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.JSON, "keep-existing-routes/apps-with-existing-routes.json"),
            },
            // (36) With new parameter - 'route'
            {
                "mtad-12.yaml", "config-01.mtaext", "/mta/cf-platform.json", null,
                false, false,
                new String[] { "foo", }, // mtaArchiveModules
                new String[] { "foo", }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"), //services
                new Expectation(Expectation.Type.JSON, "apps-12.json"),  //applications
            },
            // (37) With new parameter - 'routes'
            {
                "mtad-13.yaml", "config-01.mtaext", "/mta/cf-platform.json", null,
                false, false,
                new String[] { "foo", }, // mtaArchiveModules
                new String[] { "foo", }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"), //services
                new Expectation(Expectation.Type.JSON, "apps-13.json"),  //applications
            },
            // (38) Test plural priority over singular for hosts and domains
            {
                "mtad-14.yaml", "config-01.mtaext", "/mta/cf-platform.json", null,
                false, false,
                new String[] { "foo", }, // mtaArchiveModules
                new String[] { "foo", }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"), //services
                new Expectation(Expectation.Type.JSON, "apps-14.json"),  //applications
            },
            // (39) Test multiple buildpacks functionality
            {
                "mtad-15.yaml", "config-01.mtaext", "/mta/cf-platform.json", null,
                false, false,
                new String[] { "foo", }, // mtaArchiveModules
                new String[] { "foo", }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"), //services
                new Expectation(Expectation.Type.JSON, "apps-15.json"),  //applications
            },
// @formatter:on
        });
    }

    public CloudModelBuilderTest(String deploymentDescriptorLocation, String extensionDescriptorLocation, String platformsLocation,
                                 String deployedMtaLocation, boolean useNamespaces, boolean useNamespacesForServices,
                                 String[] mtaArchiveModules, String[] mtaModules, String[] deployedApps, Expectation expectedServices,
                                 Expectation expectedApps) {
        this.deploymentDescriptorLocation = deploymentDescriptorLocation;
        this.extensionDescriptorLocation = extensionDescriptorLocation;
        this.platformLocation = platformsLocation;
        this.deployedMtaLocation = deployedMtaLocation;
        this.useNamespaces = useNamespaces;
        this.useNamespacesForServices = useNamespacesForServices;
        this.mtaArchiveModules = new HashSet<>(Arrays.asList(mtaArchiveModules));
        this.mtaModules = new HashSet<>(Arrays.asList(mtaModules));
        this.deployedApps = new HashSet<>(Arrays.asList(deployedApps));
        this.expectedServices = expectedServices;
        this.expectedApps = expectedApps;
    }

    protected UserMessageLogger getUserMessageLogger() {
        return null;
    }

    protected DescriptorParser getDescriptorParser() {
        return getHandlerFactory().getDescriptorParser();
    }

    protected HandlerFactory getHandlerFactory() {
        return new HandlerFactory(2);
    }

    protected Map<String, Object> getParameters(Module module) {
        return module.getParameters();
    }

    protected DescriptorHandler getDescriptorHandler() {
        return getHandlerFactory().getDescriptorHandler();
    }

    protected ServicesCloudModelBuilder getServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor) {
        return new ServicesCloudModelBuilder(deploymentDescriptor);
    }

    protected ApplicationCloudModelBuilder getApplicationCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
                                                                           boolean prettyPrinting, DeployedMta deployedMta) {
        deploymentDescriptor = new DescriptorReferenceResolver(deploymentDescriptor,
                                                               new ResolverBuilder(),
                                                               new ResolverBuilder()).resolve();
        return new ApplicationCloudModelBuilder(deploymentDescriptor,
                                                prettyPrinting,
                                                deployedMta,
                                                DEPLOY_ID,
                                                Mockito.mock(UserMessageLogger.class));
    }

    protected PlatformMerger getPlatformMerger(Platform platform, DescriptorHandler handler) {
        return getHandlerFactory().getPlatformMerger(platform);
    }

    protected DescriptorMerger getDescriptorMerger() {
        return new DescriptorMerger();
    }

    @Before
    public void setUp() throws Exception {
        deploymentDescriptor = loadDeploymentDescriptor();
        ExtensionDescriptor extensionDescriptor = loadExtensionDescriptor();
        Platform platform = loadPlatform();
        DeployedMta deployedMta = loadDeployedMta();

        deploymentDescriptor = getDescriptorMerger().merge(deploymentDescriptor, Collections.singletonList(extensionDescriptor));
        PlatformMerger platformMerger = getPlatformMerger(platform, descriptorHandler);
        platformMerger.mergeInto(deploymentDescriptor);

        String defaultDomain = getDefaultDomain(platform.getName());

        insertProperNames(deploymentDescriptor);
        injectSystemParameters(deploymentDescriptor, defaultDomain);
        appBuilder = getApplicationCloudModelBuilder(deploymentDescriptor, false, deployedMta);
        servicesBuilder = getServicesCloudModelBuilder(deploymentDescriptor);

        modulesCalculator = new ModulesCloudModelBuilderContentCalculator(mtaArchiveModules,
                                                                          deployedApps,
                                                                          null,
                                                                          getUserMessageLogger(),
                                                                          new ModuleToDeployHelper(),
                                                                          Collections.singletonList(new UnresolvedModulesContentValidator(mtaModules,
                                                                                                                                          deployedApps)));

        moduleToDeployHelper = new ModuleToDeployHelper();

        resourcesCalculator = new ResourcesCloudModelBuilderContentCalculator(null, getUserMessageLogger());
    }

    private DeploymentDescriptor loadDeploymentDescriptor() {
        InputStream deploymentDescriptorYaml = getClass().getResourceAsStream(deploymentDescriptorLocation);
        return descriptorParser.parseDeploymentDescriptorYaml(deploymentDescriptorYaml);
    }

    private ExtensionDescriptor loadExtensionDescriptor() {
        InputStream extensionDescriptorYaml = getClass().getResourceAsStream(extensionDescriptorLocation);
        return descriptorParser.parseExtensionDescriptorYaml(extensionDescriptorYaml);
    }

    private Platform loadPlatform() {
        InputStream platformJson = getClass().getResourceAsStream(platformLocation);
        return configurationParser.parsePlatformJson(platformJson);
    }

    private DeployedMta loadDeployedMta() throws IOException {
        if (deployedMtaLocation == null) {
            return null;
        }
        InputStream deployedMtaStream = getClass().getResourceAsStream(deployedMtaLocation);
        String deployedMtaJson = IOUtils.toString(deployedMtaStream, StandardCharsets.UTF_8);
        return JsonUtil.fromJson(deployedMtaJson, DeployedMta.class);
    }

    protected void insertProperNames(DeploymentDescriptor descriptor) {
        insertProperAppNames(descriptor);
        insertProperServiceNames(descriptor);
    }

    private void insertProperAppNames(DeploymentDescriptor descriptor) {
        for (Module module : descriptor.getModules()) {
            String appName = computeAppName(descriptor, module);
            Map<String, Object> parameters = new TreeMap<>(module.getParameters());
            parameters.put(SupportedParameters.APP_NAME, appName);
            module.setParameters(parameters);
        }
    }

    private void insertProperServiceNames(DeploymentDescriptor descriptor) {
        for (Resource resource : descriptor.getResources()) {
            String serviceName = computeServiceName(descriptor, resource);
            Map<String, Object> parameters = new TreeMap<>(resource.getParameters());
            parameters.put(SupportedParameters.SERVICE_NAME, serviceName);
            resource.setParameters(parameters);
        }
    }

    private String computeAppName(DeploymentDescriptor descriptor, Module module) {
        String appName = (String) module.getParameters()
                                        .get(SupportedParameters.APP_NAME);
        appName = appName != null ? appName : module.getName();
        return NameUtil.computeValidApplicationName(appName, descriptor.getId(), useNamespaces);
    }

    private String computeServiceName(DeploymentDescriptor descriptor, Resource resource) {
        String serviceName = (String) resource.getParameters()
                                              .get(SupportedParameters.SERVICE_NAME);
        serviceName = serviceName != null ? serviceName : resource.getName();
        return NameUtil.computeValidServiceName(serviceName, descriptor.getId(), useNamespaces, useNamespacesForServices);
    }

    protected String getDefaultDomain(String targetName) {
        return targetName.equals("CLOUD-FOUNDRY") ? DEFAULT_DOMAIN_CF : DEFAULT_DOMAIN_XS;
    }

    protected void injectSystemParameters(DeploymentDescriptor descriptor, String defaultDomain) {
        Map<String, Object> generalSystemParameters = MapUtil.asMap(SupportedParameters.DEFAULT_DOMAIN, defaultDomain);
        descriptor.setParameters(MapUtil.merge(generalSystemParameters, descriptor.getParameters()));
        for (Module module : descriptor.getModules()) {
            Map<String, Object> moduleSystemParameters = MapUtil.asMap(SupportedParameters.DEFAULT_HOST, module.getName());
            module.setParameters(MapUtil.merge(moduleSystemParameters, module.getParameters()));
        }
    }

    @Test
    public void testGetApplications() {
        tester.test(new Callable<List<CloudApplicationExtended>>() {
            @Override
            public List<CloudApplicationExtended> call() {
                List<CloudApplicationExtended> apps = new ArrayList<>();
                List<Module> modulesToDeploy = modulesCalculator.calculateContentForBuilding(deploymentDescriptor.getModules());
                for (Module module : modulesToDeploy) {
                    apps.add(appBuilder.build(module, moduleToDeployHelper));
                }
                return apps;
            }
        }, expectedApps);
    }

    @Test
    public void testGetServices() {
        tester.test(new Callable<List<CloudServiceExtended>>() {
            @Override
            public List<CloudServiceExtended> call() {
                return servicesBuilder.build(resourcesCalculator.calculateContentForBuilding(deploymentDescriptor.getResources()));
            }
        }, expectedServices);
    }

}
