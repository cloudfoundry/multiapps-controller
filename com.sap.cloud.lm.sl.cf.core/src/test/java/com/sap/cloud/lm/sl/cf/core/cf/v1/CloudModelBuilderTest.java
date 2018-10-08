package com.sap.cloud.lm.sl.cf.core.cf.v1;

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
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v1.DeployTargetFactory;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.common.util.Callable;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;
import com.sap.cloud.lm.sl.mta.handlers.v1.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.handlers.v1.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.handlers.v1.DescriptorMerger;
import com.sap.cloud.lm.sl.mta.handlers.v1.DescriptorParser;
import com.sap.cloud.lm.sl.mta.mergers.v1.PlatformMerger;
import com.sap.cloud.lm.sl.mta.mergers.v1.TargetMerger;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1.ExtensionDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1.Module;
import com.sap.cloud.lm.sl.mta.model.v1.Platform;
import com.sap.cloud.lm.sl.mta.model.v1.Target;

@RunWith(Parameterized.class)
public class CloudModelBuilderTest {

    protected static final String DEFAULT_DOMAIN_CF = "cfapps.neo.ondemand.com";
    protected static final String DEFAULT_DOMAIN_XS = "sofd60245639a";

    protected static final String DEPLOY_ID = "123";

    protected final DescriptorParser descriptorParser = getDescriptorParser();
    protected final ConfigurationParser configurationParser = getConfigurationParser();
    protected final DescriptorHandler descriptorHandler = getDescriptorHandler();

    protected final String deploymentDescriptorLocation;
    protected final String extensionDescriptorLocation;
    protected final String platformsLocation;
    protected final String targetsLocation;
    protected final String deployedMtaLocation;
    protected final boolean useNamespaces;
    protected final boolean useNamespacesForServices;
    protected final Set<String> mtaArchiveModules;
    protected final Set<String> mtaModules;
    protected final Set<String> deployedApps;
    protected final Expectation[] expectations;

    protected ApplicationsCloudModelBuilder appsBuilder;
    protected ServicesCloudModelBuilder servicesBuilder;
    protected DomainsCloudModelBuilder domainsBuilder;

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (00) Full MTA:
            { "/mta/javahelloworld/mtad.yaml", "/mta/javahelloworld/config1.mtaext", "/mta/platform-types.json", "/mta/targets.json", null,
                false, false,
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"), // domains
                    new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/services.json"),
                    new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/apps.json"), } },
            // (01)
            { "/mta/javahelloworld/mtad.yaml", "/mta/javahelloworld/xs2-config1.mtaext", "/mta/platform-types.json", "/mta/targets.json", null,
                false, false,
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"), // domains
                    new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/xs2-services.json"),
                    new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/xs2-apps.json"), } },
            // (02) Full MTA with namespaces:
            { "/mta/javahelloworld/mtad.yaml", "/mta/javahelloworld/config1.mtaext", "/mta/platform-types.json", "/mta/targets.json", null,
                true, true,
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"), // domains
                    new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/services-ns.json"),
                    new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/apps-ns.json"), } },
            // (03) Full MTA with namespaces (w/o services):
            { "/mta/javahelloworld/mtad.yaml", "/mta/javahelloworld/config1.mtaext", "/mta/platform-types.json", "/mta/targets.json", null,
                true, false,
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"), // domains
                    new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/services.json"),
                    new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/apps-ns2.json"), } },
            // (04) Patch MTA (resolved inter-module dependencies):
            { "/mta/javahelloworld/mtad.yaml", "/mta/javahelloworld/config1.mtaext", "/mta/platform-types.json", "/mta/targets.json", null,
                false, false,
                new String[] { "java-hello-world" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // deployedApps
                new Expectation[] {
                    new Expectation("[]"), // domains
                    new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/services-patch1.json"),
                    new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/apps-patch2.json"), } },
            // (05) Patch MTA with namespaces (resolved inter-module dependencies):
            { "/mta/javahelloworld/mtad.yaml", "/mta/javahelloworld/config1.mtaext", "/mta/platform-types.json", "/mta/targets.json", null,
                true, true,
                new String[] { "java-hello-world" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // deployedApps
                new Expectation[] {
                    new Expectation("[]"), // domains
                    new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/services-patch1-ns.json"),
                    new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/apps-patch2-ns.json"), } },
            // (06) Patch MTA (unresolved inter-module dependencies):
            { "/mta/javahelloworld/mtad.yaml", "/mta/javahelloworld/config1.mtaext", "/mta/platform-types.json", "/mta/targets.json", null,
                false, false,
                new String[] { "java-hello-world" }, // mtaArchiveModules
                new String[] { "java-hello-world", "java-hello-world-db", "java-hello-world-backend" }, // mtaModules
                new String[] { "java-hello-world", }, // deployedApps
                new Expectation[] {
                    new Expectation("[]"), // domains
                    new Expectation(Expectation.Type.RESOURCE, "/mta/javahelloworld/services-patch1.json"),
                    new Expectation(Expectation.Type.EXCEPTION, "Unresolved MTA modules [java-hello-world-db, java-hello-world-backend]") } },
            // (07)
            { "/mta/shine/mtad.yaml", "/mta/shine/config1.mtaext", "/mta/platform-types.json", "/mta/targets.json", null,
                false, false,
                new String[] { "shine", "shine-xsjs", "shine-odata" }, // mtaArchiveModules
                new String[] { "shine", "shine-xsjs", "shine-odata" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"), // domains
                    new Expectation(Expectation.Type.RESOURCE, "/mta/shine/services.json"),
                    new Expectation(Expectation.Type.RESOURCE, "/mta/shine/apps.json"), } },
            // (08)
            { "/mta/sample/mtad.yaml", "/mta/sample/config1.mtaext", "/mta/sample/platform-types.json", "/mta/sample/targets.json", null,
                false, false,
                new String[] { "pricing", "pricing-db", "web-server" }, // mtaArchiveModules
                new String[] { "pricing", "pricing-db", "web-server" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[bestprice.sap.com]"), // domains
                    new Expectation(Expectation.Type.RESOURCE, "/mta/sample/services.json"),
                    new Expectation(Expectation.Type.RESOURCE, "/mta/sample/apps.json"), } },
            // (09)
            { "/mta/devxwebide/mtad.yaml", "/mta/devxwebide/config1.mtaext", "/mta/platform-types.json", "/mta/targets.json", null,
                false, false,
                new String[] { "webide" }, // mtaArchiveModules
                new String[] { "webide" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"), // domains
                    new Expectation(Expectation.Type.RESOURCE, "/mta/devxwebide/services.json"),
                    new Expectation(Expectation.Type.RESOURCE, "/mta/devxwebide/apps2.json"), } },
            // (10)
            { "/mta/devxwebide/mtad.yaml", "/mta/devxwebide/xs2-config1.mtaext", "/mta/platform-types.json", "/mta/targets.json", null,
                false, false,
                new String[] { "webide" }, // mtaArchiveModules
                new String[] { "webide" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"), // domains
                    new Expectation(Expectation.Type.RESOURCE, "/mta/devxwebide/services.json"),
                    new Expectation(Expectation.Type.RESOURCE, "/mta/devxwebide/xs2-apps2.json"), } },
            // (11)
            { "/mta/devxdi/mtad.yaml", "/mta/devxdi/config1.mtaext", "/mta/platform-types.json", "/mta/targets.json", null,
                false, false,
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaArchiveModules
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"), // domains
                    new Expectation(Expectation.Type.RESOURCE, "/mta/devxdi/services.json"),
                    new Expectation(Expectation.Type.RESOURCE, "/mta/devxdi/apps.json"), } },
            // (12)
            { "/mta/devxdi/mtad.yaml", "/mta/devxdi/xs2-config1.mtaext", "/mta/platform-types.json", "/mta/targets.json", null,
                false, false,
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaArchiveModules
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"), // domains
                    new Expectation(Expectation.Type.RESOURCE, "/mta/devxdi/xs2-services.json"),
                    new Expectation(Expectation.Type.RESOURCE, "/mta/devxdi/xs2-apps.json"), } },
            // (13)
            { "/mta/devxwebide/mtad.yaml", "/mta/devxwebide/xs2-config2.mtaext", "/mta/platform-types.json", "/mta/targets.json", null,
                false, false,
                new String[] { "webide" }, // mtaArchiveModules
                new String[] { "webide" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"), // domains
                    new Expectation(Expectation.Type.RESOURCE, "/mta/devxwebide/services.json"),
                    new Expectation(Expectation.Type.RESOURCE, "/mta/devxwebide/xs2-apps2.json"), } },
            // (14) Unknown typed resource properties:
            { "/mta/devxdi/mtad.yaml", "/mta/devxdi/xs2-config2.mtaext", "/mta/platform-types.json", "/mta/targets.json", null,
                false, false,
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaArchiveModules
                new String[] { "di-core", "di-builder", "di-runner" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"), // domains
                    new Expectation(Expectation.Type.RESOURCE, "/mta/devxdi/xs2-services.json"),
                    new Expectation(Expectation.Type.RESOURCE, "/mta/devxdi/xs2-apps.json"), } },
            // (15) Custom application names are used:
            {
                "mtad-01.yaml", "config-01.mtaext", "/mta/platform-types.json", "/mta/targets.json", null,
                false, false,
                new String[] { "module-1", "module-2" }, // mtaArchiveModules
                new String[] { "module-1", "module-2" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "apps-01.json"), }
            },
            // (16) Custom application names are used:
            {
                "mtad-01.yaml", "config-01.mtaext", "/mta/platform-types.json", "/mta/targets.json", null,
                true, true,
                new String[] { "module-1", "module-2" }, // mtaArchiveModules
                new String[] { "module-1", "module-2" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "apps-02.json"), }
            },
            // (17) Temporary URIs are used:
            {
                "mtad-03.yaml", "config-01.mtaext", "/mta/platform-types.json", "/mta/targets.json", null,
                false, false,
                new String[] { "module-1", "module-2" }, // mtaArchiveModules
                new String[] { "module-1", "module-2" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "apps-04.json"), }
            },
            // (18) Some env values have HTML symbols embedded in them:
            {
                "mtad-04.yaml", "config-01.mtaext", "/mta/platform-types.json", "/mta/targets.json", null,
                false, false,
                new String[] { "module-1" }, // mtaArchiveModules
                new String[] { "module-1" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "apps-05.json"), }
            },
            // (19) Resource service-name definition in extension descriptor:
            { 
                "mtad-05.yaml", "config-02.mtaext", "/mta/platform-types.json", "/mta/targets.json", null,
                true, true,
                new String[] { "module-1" }, // mtaArchiveModules
                new String[] { "module-1" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "services-02.json"),
                    new Expectation(Expectation.Type.RESOURCE, "apps-06.json"), }
            },
            // (20) Test support for one-off tasks:
            { 
                "mtad-06.yaml", "config-02.mtaext", "/mta/platform-types.json", "/mta/targets.json", null,
                false, false,
                new String[] { "module-1", "module-2", "module-3" }, // mtaArchiveModules
                new String[] { "module-1", "module-2", "module-3" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "apps-07.json"), }
            },
            // (21) Test inject service keys in application environment
            { 
                "mtad-11.yaml", "config-01.mtaext", "/mta/platform-types.json", "/mta/targets.json", null,
                false, false,
                new String[] { "module-1" }, // mtaArchiveModules
                new String[] { "module-1" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "apps-08.json"), }
            },
            // (22) With 'health-check-type' set to 'port':
            { 
                "mtad-health-check-type-port.yaml", "config-02.mtaext", "/mta/platform-types.json", "/mta/targets.json", null,
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "apps-with-health-check-type-port.json"), }
            },
            // (23) With 'health-check-type' set to 'http' and a non-default 'health-check-http-endpoint':
            { 
                "mtad-health-check-type-http-with-endpoint.yaml", "config-02.mtaext", "/mta/platform-types.json", "/mta/targets.json", null,
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "apps-with-health-check-type-http-with-endpoint.json"), }
            },
            // (24) With 'health-check-type' set to 'http' and no 'health-check-http-endpoint':
            { 
                "mtad-health-check-type-http-without-endpoint.yaml", "config-02.mtaext", "/mta/platform-types.json", "/mta/targets.json", null,
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "apps-with-health-check-type-http-without-endpoint.json"), }
            },
            // (25) With 'enable-ssh' set to true: 
            {
                "mtad-ssh-enabled-true.yaml", "config-02.mtaext", "/mta/platform-types.json", "/mta/targets.json", null,
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "apps-with-ssh-enabled-true.json"), }
            },
            // (26) With 'enable-ssh' set to false: 
            {
                "mtad-ssh-enabled-false.yaml", "config-02.mtaext", "/mta/platform-types.json", "/mta/targets.json", null,
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "apps-with-ssh-enabled-false.json"), }
            },
            // (28) With 'keep-existing-routes' set to true and no deployed MTA:
            {
                "keep-existing-routes/mtad.yaml", "config-02.mtaext", "/mta/platform-types.json", "/mta/targets.json", null,
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation[] {
                    new Expectation("[]"),
                    new Expectation("[]"),
                    new Expectation(Expectation.Type.RESOURCE, "keep-existing-routes/apps.json"), }
            },
            // (29) With 'keep-existing-routes' set to true and no deployed module:
            {
                "keep-existing-routes/mtad.yaml", "config-02.mtaext", "/mta/platform-types.json", "/mta/targets.json", 
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
            // (30) With 'keep-existing-routes' set to true and an already deployed module with no URIs:
            {
                "keep-existing-routes/mtad.yaml", "config-02.mtaext", "/mta/platform-types.json", "/mta/targets.json", 
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
            // (31) With 'keep-existing-routes' set to true and an already deployed module:
            {
                "keep-existing-routes/mtad.yaml", "config-02.mtaext", "/mta/platform-types.json", "/mta/targets.json", 
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
        this.deploymentDescriptorLocation = deploymentDescriptorLocation;
        this.extensionDescriptorLocation = extensionDescriptorLocation;
        this.platformsLocation = platformsLocation;
        this.targetsLocation = targetsLocation;
        this.deployedMtaLocation = deployedMtaLocation;
        this.useNamespaces = useNamespaces;
        this.useNamespacesForServices = useNamespacesForServices;
        this.mtaArchiveModules = new HashSet<>(Arrays.asList(mtaArchiveModules));
        this.mtaModules = new HashSet<>(Arrays.asList(mtaModules));
        this.deployedApps = new HashSet<>(Arrays.asList(deployedApps));
        this.expectations = expectations;
    }

    @Before
    public void setUp() throws Exception {
        DeploymentDescriptor deploymentDescriptor = loadDeploymentDescriptor();
        ExtensionDescriptor extensionDescriptor = loadExtensionDescriptor();
        Target target = loadTarget(extensionDescriptor);
        Platform platform = loadPlatform(target);
        DeployedMta deployedMta = loadDeployedMta();

        deploymentDescriptor = getDescriptorMerger().merge(deploymentDescriptor, Arrays.asList(extensionDescriptor))._1;
        insertProperAppNames(deploymentDescriptor);

        TargetMerger platformMerger = getTargetMerger(target, descriptorHandler);
        platformMerger.mergeInto(deploymentDescriptor);

        PlatformMerger platformTypeMerger = getPlatformMerger(platform, descriptorHandler);
        platformTypeMerger.mergeInto(deploymentDescriptor);

        String defaultDomain = getDefaultDomain(target.getName());

        SystemParameters systemParameters = createSystemParameters(deploymentDescriptor, defaultDomain);
        XsPlaceholderResolver xsPlaceholderResolver = new XsPlaceholderResolver();
        xsPlaceholderResolver.setDefaultDomain(defaultDomain);
        CloudModelConfiguration configuration = createCloudModelConfiguration(defaultDomain);
        appsBuilder = getApplicationsCloudModelBuilder(deploymentDescriptor, configuration, deployedMta, systemParameters,
            xsPlaceholderResolver);
        domainsBuilder = getDomainsBuilder(deploymentDescriptor, systemParameters, xsPlaceholderResolver);
        servicesBuilder = getServicesCloudModelBuilder(deploymentDescriptor, configuration);
    }

    private DeploymentDescriptor loadDeploymentDescriptor() {
        InputStream deploymentDescriptorYaml = getClass().getResourceAsStream(deploymentDescriptorLocation);
        return descriptorParser.parseDeploymentDescriptorYaml(deploymentDescriptorYaml);
    }

    private ExtensionDescriptor loadExtensionDescriptor() {
        InputStream extensionDescriptorYaml = getClass().getResourceAsStream(extensionDescriptorLocation);
        return descriptorParser.parseExtensionDescriptorYaml(extensionDescriptorYaml);
    }

    private Target loadTarget(ExtensionDescriptor extensionDescriptor) {
        InputStream targetsJson = getClass().getResourceAsStream(targetsLocation);
        List<Target> targets = configurationParser.parseTargetsJson(targetsJson);
        String targetName = extensionDescriptor.getDeployTargets()
            .get(0);
        return descriptorHandler.findTarget(targets, targetName, null);
    }

    private Platform loadPlatform(Target target) {
        InputStream platformsJson = getClass().getResourceAsStream(platformsLocation);
        List<Platform> platforms = configurationParser.parsePlatformsJson(platformsJson);
        return descriptorHandler.findPlatform(platforms, target.getType());
    }

    private DeployedMta loadDeployedMta() throws IOException {
        if (deployedMtaLocation == null) {
            return null;
        }
        InputStream deployedMtaStream = getClass().getResourceAsStream(deployedMtaLocation);
        String deployedMtaJson = IOUtils.toString(deployedMtaStream, StandardCharsets.UTF_8);
        return JsonUtil.fromJson(deployedMtaJson, DeployedMta.class);
    }

    private CloudModelConfiguration createCloudModelConfiguration(String defaultDomain) {
        CloudModelConfiguration configuration = new CloudModelConfiguration();
        configuration.setPortBasedRouting(defaultDomain.equals(DEFAULT_DOMAIN_XS));
        configuration.setPrettyPrinting(false);
        configuration.setUseNamespaces(useNamespaces);
        configuration.setUseNamespacesForServices(useNamespacesForServices);
        return configuration;
    }

    protected ServicesCloudModelBuilder getServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        CloudModelConfiguration configuration) {
        return new ServicesCloudModelBuilder(deploymentDescriptor, new HandlerFactory(1).getPropertiesAccessor(), configuration);
    }

    protected ApplicationsCloudModelBuilder getApplicationsCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        CloudModelConfiguration configuration, DeployedMta deployedMta, SystemParameters systemParameters,
        XsPlaceholderResolver xsPlaceholderResolver) {
        return new ApplicationsCloudModelBuilder(deploymentDescriptor, configuration, deployedMta, systemParameters, xsPlaceholderResolver,
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
        for (Module module : descriptor.getModules1()) {
            String moduleName = module.getName();
            moduleParameters.put(moduleName, MapUtil.asMap(SupportedParameters.DEFAULT_HOST, moduleName));
        }
        return new SystemParameters(generalParameters, moduleParameters, Collections.emptyMap(), Collections.emptyMap());
    }

    protected void insertProperAppNames(DeploymentDescriptor descriptor) throws Exception {
        for (Module module : descriptor.getModules1()) {
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
        }, expectations[0], getClass());
    }

    @Test
    public void testGetApplications() {
        TestUtil.test(new Callable<List<CloudApplicationExtended>>() {
            @Override
            public List<CloudApplicationExtended> call() throws Exception {
                return appsBuilder.build(mtaArchiveModules, mtaModules, deployedApps);
            }
        }, expectations[2], getClass(), new TestUtil.JsonSerializationOptions(false, true));
    }

    @Test
    public void testGetServices() {
        TestUtil.test(new Callable<List<CloudServiceExtended>>() {
            @Override
            public List<CloudServiceExtended> call() throws Exception {
                return servicesBuilder.build();
            }
        }, expectations[1], getClass(), new TestUtil.JsonSerializationOptions(false, true));
    }

}
