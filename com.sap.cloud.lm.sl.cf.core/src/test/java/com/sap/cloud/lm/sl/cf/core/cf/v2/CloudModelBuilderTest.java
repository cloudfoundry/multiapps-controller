package com.sap.cloud.lm.sl.cf.core.cf.v2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.util.ModulesCloudModelBuilderContentCalculator;
import com.sap.cloud.lm.sl.cf.core.cf.util.ResourcesCloudModelBuilderContentCalculator;
import com.sap.cloud.lm.sl.cf.core.helpers.ModuleToDeployHelper;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.common.util.Callable;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;
import com.sap.cloud.lm.sl.mta.handlers.v2.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorMerger;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorParser;
import com.sap.cloud.lm.sl.mta.mergers.v2.PlatformMerger;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.ExtensionDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.Module;
import com.sap.cloud.lm.sl.mta.model.v2.Platform;
import com.sap.cloud.lm.sl.mta.resolvers.ResolverBuilder;
import com.sap.cloud.lm.sl.mta.resolvers.v2.DescriptorReferenceResolver;

@RunWith(Parameterized.class)
public class CloudModelBuilderTest {

    protected static final String DEFAULT_DOMAIN_CF = "cfapps.neo.ondemand.com";
    protected static final String DEFAULT_DOMAIN_XS = "sofd60245639a";

    protected static final String DEPLOY_ID = "123";

    protected final DescriptorParser descriptorParser = getDescriptorParser();
    protected final ConfigurationParser configurationParser = getConfigurationParser();
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
    protected ResourcesCloudModelBuilderContentCalculator resourcesCalculator;

    protected ApplicationsCloudModelBuilder appsBuilder;
    protected ServicesCloudModelBuilder servicesBuilder;

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (00) Full MTA:
            { "/mta/javahelloworld/mtad-v2.yaml", "/mta/javahelloworld/config1-v2.mtaext", "/mta/cf-platform-v2.json", null,
                false, false,
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/services.json"),
                new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/apps-v2.json"),
            },
            // (01)
            { "/mta/javahelloworld/mtad-v2.yaml", "/mta/javahelloworld/xs2-config1-v2.mtaext", "/mta/xs-platform-v2.json", null,
                false, false,
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/xs2-services.json"),
                new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/xs2-apps-v2.json"),
            },
            // (02) Full MTA with namespaces:
            { "/mta/javahelloworld/mtad-v2.yaml", "/mta/javahelloworld/config1-v2.mtaext", "/mta/cf-platform-v2.json", null,
                true, true,
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/services-ns.json"),
                new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/apps-ns-v2.json"),
            },
            // (03) Full MTA with namespaces (w/o services):
            { "/mta/javahelloworld/mtad-v2.yaml", "/mta/javahelloworld/config1-v2.mtaext", "/mta/cf-platform-v2.json", null,
                true, false,
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/services.json"),
                new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/apps-ns2-v2.json"),
            },
            // (04) Patch MTA (resolved inter-module dependencies):
            { "/mta/javahelloworld/mtad-v2.yaml", "/mta/javahelloworld/config1-v2.mtaext", "/mta/cf-platform-v2.json", null,
                false, false,
                new String[] { "java-hello-world" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // deployedApps
                new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/services-patch1.json"),
                new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/apps-patch1.json"),
            },
            // (05) Patch MTA with namespaces (resolved inter-module dependencies):
            { "/mta/javahelloworld/mtad-v2.yaml", "/mta/javahelloworld/config1-v2.mtaext", "/mta/cf-platform-v2.json", null,
                true, true,
                new String[] { "java-hello-world" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // deployedApps
                new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/services-patch1-ns.json"),
                new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/apps-patch1-ns.json"),
            },
            // (06) Patch MTA (unresolved inter-module dependencies):
            { "/mta/javahelloworld/mtad-v2.yaml", "/mta/javahelloworld/config1-v2.mtaext", "/mta/cf-platform-v2.json", null,
                false, false,
                new String[] { "java-hello-world" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] { "java-hello-world", }, // deployedApps
                new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/services-patch1.json"),
                new Expectation(Expectation.Type.EXCEPTION, "Unresolved MTA modules [java-hello-world-db, java-hello-world-backend]") 
            },
            // (07)
            { "/mta/shine/mtad-v2.yaml", "/mta/shine/config1-v2.mtaext", "/mta/cf-platform-v2.json", null,
                false, false,
                new String[] { "shine", "shine-xsjs", "shine-odata" }, // mtaArchiveModules
                new String[] { "shine", "shine-xsjs", "shine-odata" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation(Expectation.Type.RESOURCE, "/mta/shine/services.json"),
                new Expectation(Expectation.Type.RESOURCE, "/mta/shine/apps-v2.json"),
            },
            // (08)
            { "/mta/sample/mtad-v2.yaml", "/mta/sample/config1-v2.mtaext", "/mta/sample/platform-v2.json", null,
                false, false,
                new String[] { "pricing", "pricing-db", "web-server" }, // mtaArchiveModules
                new String[] { "pricing", "pricing-db", "web-server" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation(Expectation.Type.RESOURCE, "/mta/sample/services.json"),
                new Expectation(Expectation.Type.RESOURCE, "/mta/sample/apps-v2.json"),
            },
            // (09)
            { "/mta/devxwebide/mtad-v2.yaml", "/mta/devxwebide/config1-v2.mtaext", "/mta/cf-platform-v2.json", null,
                false, false,
                new String[] { "webide" }, // mtaArchiveModules
                new String[] { "webide" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation(Expectation.Type.RESOURCE, "/mta/devxwebide/services.json"),
                new Expectation(Expectation.Type.RESOURCE, "/mta/devxwebide/apps.json"),
            },
            // (10)
            { "/mta/devxwebide/mtad-v2.yaml", "/mta/devxwebide/xs2-config1-v2.mtaext", "/mta/xs-platform-v2.json", null,
                false, false,
                new String[] { "webide" }, // mtaArchiveModules
                new String[] { "webide" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation(Expectation.Type.RESOURCE, "/mta/devxwebide/services.json"),
                new Expectation(Expectation.Type.RESOURCE, "/mta/devxwebide/xs2-apps.json"),
            },
            // (11)
            { "/mta/devxdi/mtad-v2.yaml", "/mta/devxdi/config1-v2.mtaext", "/mta/cf-platform-v2.json", null,
                false, false,
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaArchiveModules
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation(Expectation.Type.RESOURCE, "/mta/devxdi/services.json"),
                new Expectation(Expectation.Type.RESOURCE, "/mta/devxdi/apps-v2.json"),
            },
            // (12)
            { "/mta/devxdi/mtad-v2.yaml", "/mta/devxdi/xs2-config1-v2.mtaext", "/mta/xs-platform-v2.json", null,
                false, false,
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaArchiveModules
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation(Expectation.Type.RESOURCE, "/mta/devxdi/xs2-services.json"),
                new Expectation(Expectation.Type.RESOURCE, "/mta/devxdi/xs2-apps-v2.json"),
            },
            // (13)
            { "/mta/devxwebide/mtad-v2.yaml", "/mta/devxwebide/xs2-config2-v2.mtaext", "/mta/xs-platform-v2.json", null,
                false, false,
                new String[] { "webide" }, // mtaArchiveModules
                new String[] { "webide" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation(Expectation.Type.RESOURCE, "/mta/devxwebide/services.json"),
                new Expectation(Expectation.Type.RESOURCE, "/mta/devxwebide/xs2-apps.json"), 
            },
            // (14) Unknown typed resource parameters:
            { "/mta/devxdi/mtad-v2.yaml", "/mta/devxdi/xs2-config2-v2.mtaext", "/mta/xs-platform-v2.json", null,
                false, false,
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaArchiveModules
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation(Expectation.Type.RESOURCE, "/mta/devxdi/xs2-services.json"),
                new Expectation(Expectation.Type.RESOURCE, "/mta/devxdi/xs2-apps-v2.json"),
            },
            // (15) Service binding parameters in requires dependency:
            { "mtad-01.yaml", "config-01.mtaext", "/mta/cf-platform-v2.json", null,
                false, false,
                new String[] { "foo", }, // mtaArchiveModules
                new String[] { "foo", }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.RESOURCE, "apps-01.json"),
            },
            // (16) Service binding parameters in requires dependency:
            { "mtad-02.yaml", "config-01.mtaext", "/mta/cf-platform-v2.json", null,
                false, false,
                new String[] { "foo", }, // mtaArchiveModules
                new String[] { "foo", }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.EXCEPTION, "Invalid type for key \"foo#bar#config\", expected \"Map\" but got \"String\""),
            },
            // (17) Custom application names are used:
            {
                "mtad-03.yaml", "config-02.mtaext", "/mta/xs-platform-v2.json", null,
                false, false,
                new String[] { "module-1", "module-2" }, // mtaArchiveModules
                new String[] { "module-1", "module-2" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.RESOURCE, "apps-02.json"),
            },
            // (18) Custom application names are used:
            {
                "mtad-03.yaml", "config-02.mtaext", "/mta/xs-platform-v2.json", null,
                true, true,
                new String[] { "module-1", "module-2" }, // mtaArchiveModules
                new String[] { "module-1", "module-2" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.RESOURCE, "apps-03.json"),
            },
            // (19) Temporary URIs are used:
            {
                "mtad-05.yaml", "config-02.mtaext", "/mta/xs-platform-v2.json", null,
                false, false,
                new String[] { "module-1", "module-2" }, // mtaArchiveModules
                new String[] { "module-1", "module-2" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.RESOURCE, "apps-05.json"),
            },
            // (20) Use list parameter:
            {
                "mtad-06.yaml", "config-02.mtaext", "/mta/xs-platform-v2.json", null,
                false, false,
                new String[] { "framework" }, // mtaArchiveModules
                new String[] { "framework" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.RESOURCE, "apps-06.json"),
            },
            // (21) Use partial plugin:
            {
                "mtad-07.yaml", "config-02.mtaext", "/mta/xs-platform-v2.json", null,
                false, false,
                new String[] { "framework" }, // mtaArchiveModules
                new String[] { "framework" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.RESOURCE, "apps-07.json"),
            },
            // (22) Overwrite service-name resource property in ext. descriptor:
            {
                "mtad-08.yaml", "config-03.mtaext", "/mta/xs-platform-v2.json", null,
                false, false,
                new String[] { "module-1" }, // mtaArchiveModules
                new String[] { "module-1" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation(Expectation.Type.RESOURCE, "services-03.json"),
                new Expectation(Expectation.Type.RESOURCE, "apps-08.json"),
            },
            // (23) Test support for one-off tasks:
            {
                "mtad-09.yaml", "config-03.mtaext", "/mta/xs-platform-v2.json", null,
                false, false,
                new String[] { "module-1", "module-2", "module-3", "module-4" }, // mtaArchiveModules
                new String[] { "module-1", "module-2", "module-3", "module-4" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.RESOURCE, "apps-09.json"),
            },
            // (24) With 'health-check-type' set to 'port':
            { 
                "mtad-health-check-type-port.yaml", "config-03.mtaext", "/mta/xs-platform-v2.json", null,
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.RESOURCE, "apps-with-health-check-type-port.json"),
            },
            // (25) With 'health-check-type' set to 'http' and a non-default 'health-check-http-endpoint':
            { 
                "mtad-health-check-type-http-with-endpoint.yaml", "config-03.mtaext", "/mta/xs-platform-v2.json", null,
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.RESOURCE, "apps-with-health-check-type-http-with-endpoint.json"),
            },
            // (26) With 'health-check-type' set to 'http' and no 'health-check-http-endpoint':
            { 
                "mtad-health-check-type-http-without-endpoint.yaml", "config-03.mtaext", "/mta/xs-platform-v2.json", null,
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.RESOURCE, "apps-with-health-check-type-http-without-endpoint.json"),
            },
            // (27) Test inject service keys:
            {
                "mtad-10.yaml", "config-02.mtaext", "/mta/xs-platform-v2.json", null,
                false, false,
                new String[] { "module-1" }, // mtaArchiveModules
                new String[] { "module-1" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.RESOURCE, "apps-10.json"),
            },
            // (28) With 'enable-ssh' set to true: 
            {
                "mtad-ssh-enabled-true.yaml", "config-02.mtaext", "/mta/xs-platform-v2.json", null,
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.RESOURCE, "apps-with-ssh-enabled-true.json"),
            },
            // (29) With 'enable-ssh' set to false: 
            {
                "mtad-ssh-enabled-false.yaml", "config-02.mtaext", "/mta/xs-platform-v2.json", null,
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.RESOURCE, "apps-with-ssh-enabled-false.json"),
            },
            // (30) With TCPS routes
            {
                "mtad-11.yaml", "config-02.mtaext", "/mta/xs-platform-v2.json", null,
                false, false,
                new String[] { "module-1", "module-2", "module-3" }, // mtaArchiveModules
                new String[] { "module-1", "module-2", "module-3" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.RESOURCE, "apps-with-tcp-routes.json"),
            },
            // (31) Do not restart on env change - bg-deploy
            { "mtad-restart-on-env-change.yaml", "config-02.mtaext", "/mta/xs-platform-v2.json", null,
                false, false, 
                new String[] { "module-1", "module-2", "module-3" }, // mtaArchiveModules
                new String[] { "module-1", "module-2", "module-3" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.RESOURCE, "apps-with-restart-parameters-false.json") // services
            },
            // (32) With 'keep-existing-routes' set to true and no deployed MTA:
            {
                "keep-existing-routes/mtad.yaml", "config-02.mtaext", "/mta/xs-platform-v2.json", null,
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.RESOURCE, "keep-existing-routes/apps.json"),
            },
            // (33) With 'keep-existing-routes' set to true and no deployed module:
            {
                "keep-existing-routes/mtad.yaml", "config-02.mtaext", "/mta/xs-platform-v2.json", 
                "keep-existing-routes/deployed-mta-without-foo-module.json",
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.RESOURCE, "keep-existing-routes/apps.json"),
            },
            // (34) With 'keep-existing-routes' set to true and an already deployed module with no URIs:
            {
                "keep-existing-routes/mtad.yaml", "config-02.mtaext", "/mta/xs-platform-v2.json", 
                "keep-existing-routes/deployed-mta-without-uris.json",
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.RESOURCE, "keep-existing-routes/apps.json"),
            },
            // (35) With 'keep-existing-routes' set to true and an already deployed module:
            {
                "keep-existing-routes/mtad.yaml", "config-02.mtaext", "/mta/xs-platform-v2.json", 
                "keep-existing-routes/deployed-mta.json",
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.RESOURCE, "keep-existing-routes/apps-with-existing-routes.json"),
            },
            // (36) With global 'keep-existing-routes' set to true and an already deployed module:
            {
                "keep-existing-routes/mtad-with-global-parameter.yaml", "config-02.mtaext", "/mta/xs-platform-v2.json", 
                "keep-existing-routes/deployed-mta.json",
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.RESOURCE, "keep-existing-routes/apps-with-existing-routes.json"),
            },
            // (37) With new parameter - 'route'
            {
                "mtad-12.yaml", "config-01.mtaext", "/mta/cf-platform-v2.json", null,
                false, false,
                new String[] { "foo", }, // mtaArchiveModules
                new String[] { "foo", }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"), //services
                new Expectation(Expectation.Type.RESOURCE, "apps-12.json"),  //applications
            },
            // (38) With new parameter - 'routes'
            {
                "mtad-13.yaml", "config-01.mtaext", "/mta/cf-platform-v2.json", null,
                false, false,
                new String[] { "foo", }, // mtaArchiveModules
                new String[] { "foo", }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"), //services
                new Expectation(Expectation.Type.RESOURCE, "apps-13.json"),  //applications
            },
            // (39) With parameter - 'route', using tcp
            {
                "mtad-14.yaml", "config-01.mtaext", "/mta/cf-platform-v2.json", null,
                false, false,
                new String[] { "foo", }, // mtaArchiveModules
                new String[] { "foo", }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"), //services
                new Expectation(Expectation.Type.RESOURCE, "apps-14.json"),  //applications
            },
// @formatter:on
        });
    }

    public CloudModelBuilderTest(String deploymentDescriptorLocation, String extensionDescriptorLocation, String platformsLocation,
        String deployedMtaLocation, boolean useNamespaces, boolean useNamespacesForServices, String[] mtaArchiveModules,
        String[] mtaModules, String[] deployedApps, Expectation expectedServices, Expectation expectedApps) {
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

    protected ConfigurationParser getConfigurationParser() {
        return getHandlerFactory().getConfigurationParser();
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

    protected ServicesCloudModelBuilder getServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        CloudModelConfiguration configuration) {
        return new ServicesCloudModelBuilder(deploymentDescriptor, getPropertiesAccessor(), configuration);
    }

    protected ApplicationsCloudModelBuilder getApplicationsCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        CloudModelConfiguration configuration, DeployedMta deployedMta, SystemParameters systemParameters,
        XsPlaceholderResolver xsPlaceholderResolver) {
        deploymentDescriptor = new DescriptorReferenceResolver(deploymentDescriptor, new ResolverBuilder(), new ResolverBuilder())
            .resolve();
        return new ApplicationsCloudModelBuilder(deploymentDescriptor, configuration, deployedMta, systemParameters, xsPlaceholderResolver,
            DEPLOY_ID);
    }

    protected PlatformMerger getPlatformMerger(Platform platform, DescriptorHandler handler) {
        return getHandlerFactory().getPlatformMerger(platform);
    }

    protected void setParameters(Module module, Map<String, Object> parameters) {
        module.setParameters(parameters);
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

        deploymentDescriptor = getDescriptorMerger().merge(deploymentDescriptor, Arrays.asList(extensionDescriptor));
        insertProperAppNames(deploymentDescriptor);
        PlatformMerger platformMerger = getPlatformMerger(platform, descriptorHandler);
        platformMerger.mergeInto(deploymentDescriptor);

        String defaultDomain = getDefaultDomain(platform.getName());

        SystemParameters systemParameters = createSystemParameters(deploymentDescriptor, defaultDomain);
        XsPlaceholderResolver xsPlaceholderResolver = new XsPlaceholderResolver();
        xsPlaceholderResolver.setDefaultDomain(defaultDomain);
        CloudModelConfiguration configuration = createCloudModelConfiguration(defaultDomain);
        appsBuilder = getApplicationsCloudModelBuilder(deploymentDescriptor, configuration, deployedMta, systemParameters,
            xsPlaceholderResolver);
        servicesBuilder = getServicesCloudModelBuilder(deploymentDescriptor, configuration);

        modulesCalculator = new ModulesCloudModelBuilderContentCalculator(mtaArchiveModules, deployedApps, mtaModules,
            Collections.emptyList(), getPropertiesAccessor(), getUserMessageLogger(), new ModuleToDeployHelper());

        resourcesCalculator = new ResourcesCloudModelBuilderContentCalculator(Collections.emptyList(), getPropertiesAccessor(),
            getUserMessageLogger());
    }

    protected PropertiesAccessor getPropertiesAccessor() {
        return getHandlerFactory().getPropertiesAccessor();
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

    protected void insertProperAppNames(DeploymentDescriptor descriptor) throws Exception {
        for (Module module : descriptor.getModules2()) {
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

    protected String getDefaultDomain(String targetName) {
        return (targetName.equals("CLOUD-FOUNDRY")) ? DEFAULT_DOMAIN_CF : DEFAULT_DOMAIN_XS;
    }

    protected SystemParameters createSystemParameters(DeploymentDescriptor descriptor, String defaultDomain) {
        Map<String, Object> generalParameters = new HashMap<>();
        generalParameters.put(SupportedParameters.DEFAULT_DOMAIN, defaultDomain);
        Map<String, Map<String, Object>> moduleParameters = new HashMap<>();
        for (Module module : descriptor.getModules2()) {
            String moduleName = module.getName();
            moduleParameters.put(moduleName, MapUtil.asMap(SupportedParameters.DEFAULT_HOST, moduleName));
        }
        return new SystemParameters(generalParameters, moduleParameters, Collections.emptyMap(), Collections.emptyMap());
    }

    private CloudModelConfiguration createCloudModelConfiguration(String defaultDomain) {
        CloudModelConfiguration configuration = new CloudModelConfiguration();
        configuration.setPortBasedRouting(defaultDomain.equals(DEFAULT_DOMAIN_XS));
        configuration.setPrettyPrinting(false);
        configuration.setUseNamespaces(useNamespaces);
        configuration.setUseNamespacesForServices(useNamespacesForServices);
        return configuration;
    }

    @Test
    public void testGetApplications() {
        TestUtil.test(new Callable<List<CloudApplicationExtended>>() {
            @Override
            public List<CloudApplicationExtended> call() throws Exception {
                return appsBuilder.build(modulesCalculator.calculateContentForBuilding(deploymentDescriptor.getModules2()), new ModuleToDeployHelper());
            }
        }, expectedApps, getClass(), new TestUtil.JsonSerializationOptions(false, true));
    }

    @Test
    public void testGetServices() {
        TestUtil.test(new Callable<List<CloudServiceExtended>>() {
            @Override
            public List<CloudServiceExtended> call() throws Exception {
                return servicesBuilder.build(resourcesCalculator.calculateContentForBuilding(deploymentDescriptor.getResources2()));
            }
        }, expectedServices, getClass(), new TestUtil.JsonSerializationOptions(false, true));
    }

}
