package com.sap.cloud.lm.sl.cf.process.steps;

import static com.sap.cloud.lm.sl.cf.process.steps.StepsTestUtil.loadDeploymentDescriptor;
import static com.sap.cloud.lm.sl.cf.process.steps.StepsTestUtil.loadPlatformTypes;
import static com.sap.cloud.lm.sl.cf.process.steps.StepsTestUtil.loadPlatforms;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;

import com.google.gson.reflect.TypeToken;
import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceKey;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.CloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.util.ProcessConflictPreventer;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorParser;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatformType;

@RunWith(Parameterized.class)
public class BuildCloudDeployModelStepTest extends AbstractStepTest<BuildCloudDeployModelStep> {

    private static final ConfigurationParser CONFIGURATION_PARSER = new ConfigurationParser();
    private static final DescriptorParser DESCRIPTOR_PARSER = new DescriptorParser();

    private static final Integer MTA_MAJOR_SCHEMA_VERSION = 1;
    private static final Integer MTA_MINOR_SCHEMA_VERSION = 0;

    private static final DeploymentDescriptor DEPLOYMENT_DESCRIPTOR = loadDeploymentDescriptor(DESCRIPTOR_PARSER, "node-hello-mtad.yaml",
        BuildCloudDeployModelStepTest.class);
    private static final TargetPlatformType PLATFORM_TYPE = loadPlatformTypes(CONFIGURATION_PARSER, "platform-types-01.json",
        BuildCloudDeployModelStepTest.class).get(0);
    private static final TargetPlatform PLATFORM = loadPlatforms(CONFIGURATION_PARSER, "platforms-01.json",
        BuildCloudDeployModelStepTest.class).get(0);

    private static final SystemParameters EMPTY_SYSTEM_PARAMETERS = new SystemParameters(Collections.emptyMap(), Collections.emptyMap(),
        Collections.emptyMap(), Collections.emptyMap());

    private static class StepInput {

        public String servicesToCreateLocation;
        public String deployedMtaLocation;
        public String serviceKeysLocation;
        public String appsToDeployLocation;
        public List<String> customDomains;

        public StepInput(String appsToDeployLocation, String servicesToCreateLocation, String serviceKeysLocation,
            List<String> customDomains, String deployedMtaLocation) {
            this.servicesToCreateLocation = servicesToCreateLocation;
            this.deployedMtaLocation = deployedMtaLocation;
            this.serviceKeysLocation = serviceKeysLocation;
            this.appsToDeployLocation = appsToDeployLocation;
            this.customDomains = customDomains;
        }

    }

    private static class StepOutput {

        public String newMtaVersion;

        public StepOutput(String newMtaVersion) {
            this.newMtaVersion = newMtaVersion;
        }

    }

    private class BuildCloudDeployModelStepMock extends BuildCloudDeployModelStep {

        @Override
        protected CloudModelBuilder getCloudModelBuilder(HandlerFactory handlerFactory, DeploymentDescriptor descriptor,
            SystemParameters systemParameters, boolean portBasedRouting, boolean prettyPrinting, boolean useNamespaces,
            boolean useNamespacesForServices, boolean allowInvalidEnvNames, String deployId, XsPlaceholderResolver xsPlaceholderResolver) {
            return cloudModelBuilder;
        }

    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            {
                new StepInput("apps-to-deploy-05.json", "services-to-create-01.json", "service-keys-01.json", Arrays.asList("custom-domain-1", "custom-domain-2"), "deployed-mta-12.json"), new StepOutput("0.1.0"),
            },
            {
                new StepInput("apps-to-deploy-05.json", "services-to-create-01.json", "service-keys-01.json", Arrays.asList("custom-domain-1", "custom-domain-2"), null), new StepOutput("0.1.0"),
            },
// @formatter:on
        });
    }

    private StepOutput output;
    private StepInput input;

    private List<CloudApplicationExtended> appsToDeploy;
    private DeployedMta deployedMta;
    private List<CloudServiceExtended> servicesToCreate;
    private Map<String, List<ServiceKey>> serviceKeys;

    @Mock
    private CloudModelBuilder cloudModelBuilder;

    public BuildCloudDeployModelStepTest(StepInput input, StepOutput output) {
        this.output = output;
        this.input = input;
    }

    @Before
    public void setUp() throws Exception {
        loadParameters();
        prepareContext();
    }

    private void prepareContext() throws Exception {
        step.conflictPreventerSupplier = (dao) -> mock(ProcessConflictPreventer.class);
        context.setVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION, MTA_MAJOR_SCHEMA_VERSION);
        context.setVariable(Constants.VAR_MTA_MINOR_SCHEMA_VERSION, MTA_MINOR_SCHEMA_VERSION);

        StepsUtil.setSystemParameters(context, EMPTY_SYSTEM_PARAMETERS);
        StepsUtil.setMtaModules(context, Collections.emptySet());
        StepsUtil.setMtaArchiveModules(context, Collections.emptySet());
        StepsUtil.setDeploymentDescriptor(context, DEPLOYMENT_DESCRIPTOR);
        StepsUtil.setXsPlaceholderReplacementValues(context, getDummyReplacementValues());

        StepsUtil.setPlatformType(context, PLATFORM_TYPE);
        StepsUtil.setPlatform(context, PLATFORM);
    }

    private Map<String, Object> getDummyReplacementValues() {
        Map<String, Object> result = new TreeMap<>();
        result.put(SupportedParameters.XSA_ROUTER_PORT_PLACEHOLDER, 0);
        return result;
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertEquals(ExecutionStatus.SUCCESS.toString(),
            context.getVariable(com.sap.activiti.common.Constants.STEP_NAME_PREFIX + step.getLogicalStepName()));

        TestUtil.test(() -> StepsUtil.getServicesToCreate(context), "R:" + input.servicesToCreateLocation, getClass());
        TestUtil.test(() -> StepsUtil.getServiceKeysToCreate(context), "R:" + input.serviceKeysLocation, getClass());
        TestUtil.test(() -> StepsUtil.getAppsToDeploy(context), "R:" + input.appsToDeployLocation, getClass());
        assertEquals(input.customDomains, StepsUtil.getCustomDomains(context));

        assertEquals(output.newMtaVersion, StepsUtil.getNewMtaVersion(context));
    }

    private void loadParameters() throws Exception {
        String appsToDeployString = TestUtil.getResourceAsString(input.appsToDeployLocation, getClass());
        appsToDeploy = JsonUtil.fromJson(appsToDeployString, new TypeToken<List<CloudApplicationExtended>>() {
        }.getType());

        String servicesToCreateString = TestUtil.getResourceAsString(input.servicesToCreateLocation, getClass());
        servicesToCreate = JsonUtil.fromJson(servicesToCreateString, new TypeToken<List<CloudServiceExtended>>() {
        }.getType());

        String serviceKeysString = TestUtil.getResourceAsString(input.serviceKeysLocation, getClass());
        serviceKeys = JsonUtil.fromJson(serviceKeysString, new TypeToken<Map<String, List<ServiceKey>>>() {
        }.getType());

        if (input.deployedMtaLocation != null) {
            String deployedMtaString = TestUtil.getResourceAsString(input.deployedMtaLocation, getClass());
            deployedMta = JsonUtil.fromJson(deployedMtaString, DeployedMta.class);
        }

        when(cloudModelBuilder.getApplications(any(), any(), any())).thenReturn(appsToDeploy);
        when(cloudModelBuilder.getCustomDomains()).thenReturn(input.customDomains);
        when(cloudModelBuilder.getServices(any())).thenReturn(servicesToCreate);
        when(cloudModelBuilder.getServiceKeys()).thenReturn(serviceKeys);
        StepsUtil.setDeployedMta(context, deployedMta);
    }

    @Override
    protected BuildCloudDeployModelStep createStep() {
        return new BuildCloudDeployModelStepMock();
    }

}
