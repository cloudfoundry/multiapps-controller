package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudEntity.Meta;
import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudInfoExtended;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;
import com.sap.cloud.lm.sl.cf.core.helpers.CredentialsGenerator;
import com.sap.cloud.lm.sl.cf.core.helpers.PortAllocator;
import com.sap.cloud.lm.sl.cf.core.helpers.PortAllocatorMock;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.PortValidator;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.mta.model.VersionRule;
import com.sap.cloud.lm.sl.mta.model.v1.DeploymentDescriptor;

@RunWith(Parameterized.class)
public class CollectSystemParametersStepTest extends SyncActivitiStepTest<CollectSystemParametersStep> {

    private static final String GENERATED_CREDENTIAL = "credential";
    private static final UUID CLOUD_DOMAIN_GUID = UUID.fromString("7b5987e9-4325-4bb6-93e2-a0b1c562e60c");
    private static final String DEFAULT_TIMESTAMP = "19700101";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    protected static class StepInput {

        public String deploymentDescriptorLocation;
        public String authorizationEndpoint;
        public String deployServiceUrl;
        public String defaultDomain;
        public boolean portBasedRouting;
        public boolean useNamespaces;
        public boolean useNamespacesForServices;
        public String deployedMtaLocation;
        public String user;
        public String platformName;
        public String org;
        public String space;
        public int majorMtaSchemaVersion;
        public PlatformType xsType;
        public boolean areXsPlaceholdersSupported;

        public StepInput(String deploymentDescriptorLocation, String authorizationEndpoint, String deployServiceUrl, String defaultDomain,
            boolean portBasedRouting, boolean useNamespaces, boolean useNamespacesForServices, String user, String platformName, String org,
            String space, int majorMtaSchemaVersion, String deployedMtaLocation, PlatformType xsType,
            boolean areXsPlaceholdersSupported) {
            this.deploymentDescriptorLocation = deploymentDescriptorLocation;
            this.authorizationEndpoint = authorizationEndpoint;
            this.deployServiceUrl = deployServiceUrl;
            this.defaultDomain = defaultDomain;
            this.portBasedRouting = portBasedRouting;
            this.useNamespaces = useNamespaces;
            this.useNamespacesForServices = useNamespacesForServices;
            this.deployedMtaLocation = deployedMtaLocation;
            this.user = user;
            this.platformName = platformName;
            this.org = org;
            this.space = space;
            this.majorMtaSchemaVersion = majorMtaSchemaVersion;
            this.xsType = xsType;
            this.areXsPlaceholdersSupported = areXsPlaceholdersSupported;
        }

    }

    protected static class StepOutput {

        public Set<Integer> allocatedPorts;
        public String systemParametersLocation;
        public String versionException;

        public StepOutput(Set<Integer> allocatedPorts, String systemParametersLocation, String versionException) {
            this.allocatedPorts = allocatedPorts;
            this.systemParametersLocation = systemParametersLocation;
            this.versionException = versionException;
        }
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Should not use namespaces for applications and services:
            {
                new StepInput("node-hello-mtad.yaml", "https://localhost:30032/uaa-security", "https://deploy-service-url:51002", "localhost", true , false, false, "XSMASTER", "initial initial", "initial", "initial", 1, null, PlatformType.XS2, false), 
                new StepOutput(new TreeSet<>(Arrays.asList(1, 2, 3)), "R:system-parameters-02.json", null),
            },
            // (1) Should use namespaces for applications and services:
            {
                new StepInput("node-hello-mtad.yaml", "https://localhost:30032/uaa-security", "https://deploy-service-url:51002", "localhost", true , true , true , "XSMASTER", "initial initial", "initial", "initial", 1, null, PlatformType.XS2, false), 
                new StepOutput(new TreeSet<>(Arrays.asList(1, 2, 3)), "R:system-parameters-01.json", null),
            },
            // (2) There are deployed MTAs:
            {
                new StepInput("node-hello-mtad.yaml", "https://localhost:30032/uaa-security", "https://deploy-service-url:51002", "localhost", true , true , true , "XSMASTER", "initial initial", "initial", "initial", 1, "deployed-mta-01.json", PlatformType.XS2, false), 
                new StepOutput(new TreeSet<>(Arrays.asList(1, 2, 3)), "R:system-parameters-04.json", null),
            },
            // (3) Host based routing:
            {
                new StepInput("node-hello-mtad.yaml", "https://localhost:30032/uaa-security", "https://deploy-service-url:51002", "localhost", false, true , true , "XSMASTER", "initial initial", "initial", "initial", 1, null, PlatformType.XS2, false), 
                new StepOutput(null, "R:system-parameters-03.json", null),
            },
            // (4) The version of the MTA is lower than the version of the previously deployed MTA:
            {
                new StepInput("node-hello-mtad.yaml", "https://localhost:30032/uaa-security", "https://deploy-service-url:51002", "localhost", true , true , true , "XSMASTER", "initial initial", "initial", "initial", 1, "deployed-mta-02.json", PlatformType.XS2, false), 
                new StepOutput(Collections.emptySet(), "R:system-parameters-04.json", Messages.HIGHER_VERSION_ALREADY_DEPLOYED),
            },
            // (5) Should not use namespaces for applications and services (platform type  is  CF):
            {
                new StepInput("node-hello-mtad.yaml", "https://localhost:30032/uaa-security", "https://deploy-service-url:51002", "localhost", true , false, false, "XSMASTER", "initial initial", "initial", "initial", 1, null, PlatformType.CF , false), 
                new StepOutput(new TreeSet<>(Arrays.asList(1, 2, 3)), "R:system-parameters-07.json", null),
            },
            // (6) Should not use namespaces for applications and services (XS placeholders are supported):
            {
                new StepInput("node-hello-mtad.yaml", "https://localhost:30032/uaa-security", "https://deploy-service-url:51002", "localhost", true , false, false, "XSMASTER", "initial initial", "initial", "initial", 1, null, PlatformType.XS2, true ), 
                new StepOutput(new TreeSet<>(Arrays.asList(1, 2, 3)), "R:system-parameters-06.json", null),
            },
            // (7) Host based routing with TCP/TCPS
            {
                new StepInput("mtad-tcp-tcps.yaml", "https://localhost:30032/uaa-security", "https://deploy-service-url:51002", "localhost", false, true , true , "XSMASTER", "initial initial", "initial", "initial", 3, null, PlatformType.XS2, false), 
                new StepOutput(new TreeSet<>(Arrays.asList(1, 2)), "R:system-parameters-12.json", null),
            },
            // (8) Host based routing with TCP/TCPS with existing apps with HTTP uris
            {
                new StepInput("mtad-tcp-tcps.yaml", "https://localhost:30032/uaa-security", "https://deploy-service-url:51002", "localhost", false, true , true , "XSMASTER", "initial initial", "initial", "initial", 3, "deployed-mta-13.json", PlatformType.XS2, false), 
                new StepOutput(new TreeSet<>(Arrays.asList(1, 2)), "R:system-parameters-12.json", null),
            },
            // (9) Host based routing with TCP/TCPS with existing apps with TCP uris
            {
                new StepInput("mtad-tcp-tcps.yaml", "https://localhost:30032/uaa-security", "https://deploy-service-url:51002", "localhost", false, true , true , "XSMASTER", "initial initial", "initial", "initial", 3, "deployed-mta-14.json", PlatformType.XS2, false), 
                new StepOutput(Collections.emptySet(), "R:system-parameters-13.json", null),
            },
// @formatter:on
        });
    }

    private DeploymentDescriptor descriptor;
    private DeployedMta deployedMta;

    private StepOutput output;
    private StepInput input;

    private PortAllocator portAllocator = new PortAllocatorMock(PortValidator.MIN_PORT_VALUE);
    @Mock
    private CredentialsGenerator credentialsGenerator;

    public CollectSystemParametersStepTest(StepInput input, StepOutput output) {
        this.input = input;
        this.output = output;
    }

    @Before
    public void setUp() throws Exception {
        loadParameters();
        prepareContext();
        prepareClient();
        when(credentialsGenerator.next(anyInt())).thenReturn(GENERATED_CREDENTIAL);

        if (output.versionException != null) {
            expectedException.expect(SLException.class);
            expectedException.expectMessage(output.versionException);
        }
    }

    private void loadParameters() throws Exception {
        String deploymentDescriptorString = TestUtil.getResourceAsString(input.deploymentDescriptorLocation, getClass());

        descriptor = new HandlerFactory(input.majorMtaSchemaVersion).getDescriptorParser()
            .parseDeploymentDescriptorYaml(deploymentDescriptorString);
        if (input.deployedMtaLocation != null) {
            String deployedMtaString = TestUtil.getResourceAsString(input.deployedMtaLocation, getClass());
            deployedMta = JsonUtil.fromJson(deployedMtaString, DeployedMta.class);
        }
    }

    private void prepareContext() throws Exception {
        when(configuration.getPlatformType()).thenReturn(input.xsType);
        when(configuration.getTargetURL()).thenReturn(ApplicationConfiguration.DEFAULT_TARGET_URL);
        when(configuration.getRouterPort()).thenReturn(ApplicationConfiguration.DEFAULT_HTTP_ROUTER_PORT);
        when(configuration.areXsPlaceholdersSupported()).thenReturn(input.areXsPlaceholdersSupported);
        step.credentialsGeneratorSupplier = () -> credentialsGenerator;
        step.timestampSupplier = () -> DEFAULT_TIMESTAMP;

        context.setVariable(Constants.VAR_SPACE, input.space);
        context.setVariable(Constants.VAR_ORG, input.org);

        context.setVariable(Constants.PARAM_USE_NAMESPACES_FOR_SERVICES, input.useNamespacesForServices);
        context.setVariable(Constants.PARAM_USE_NAMESPACES, input.useNamespaces);
        context.setVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION, input.majorMtaSchemaVersion);

        StepsUtil.setUnresolvedDeploymentDescriptor(context, descriptor);
        context.setVariable(Constants.PARAM_TARGET_NAME, input.platformName);

        context.setVariable(Constants.PARAM_VERSION_RULE, VersionRule.SAME_HIGHER.toString());
        context.setVariable(Constants.VAR_USER, input.user);

        StepsUtil.setDeployedMta(context, deployedMta);
    }

    private void prepareClient() throws Exception {
        CloudDomain domain = mock(CloudDomain.class);
        CloudInfo info;
        if (input.portBasedRouting) {
            info = mock(CloudInfoExtended.class);
            when(((CloudInfoExtended) info).isPortBasedRouting()).thenReturn(true);
        } else {
            info = mock(CloudInfo.class);
        }

        if (info instanceof CloudInfoExtended)
            when(((CloudInfoExtended) info).getDeployServiceUrl()).thenReturn(input.deployServiceUrl);

        when(clientProvider.getPortAllocator(any(), anyString())).thenReturn(portAllocator);

        when(info.getAuthorizationEndpoint()).thenReturn(input.authorizationEndpoint);
        when(domain.getName()).thenReturn(input.defaultDomain);
        when(domain.getMeta()).thenReturn(new Meta(CLOUD_DOMAIN_GUID, null, null));

        when(client.getDefaultDomain()).thenReturn(domain);
        when(client.getCloudInfo()).thenReturn(info);
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();

        if (output.allocatedPorts != null) {
            assertEquals(output.allocatedPorts, StepsUtil.getAllocatedPorts(context));
        }

        TestUtil.test(() -> {

            return StepsUtil.getSystemParameters(context);

        }, output.systemParametersLocation, getClass());
    }

    @Override
    protected CollectSystemParametersStep createStep() {
        return new CollectSystemParametersStep();
    }

}
