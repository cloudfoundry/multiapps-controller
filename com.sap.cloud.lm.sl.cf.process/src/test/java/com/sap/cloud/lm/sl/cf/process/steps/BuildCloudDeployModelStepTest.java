package com.sap.cloud.lm.sl.cf.process.steps;

import static com.sap.cloud.lm.sl.cf.process.steps.StepsTestUtil.loadDeploymentDescriptor;
import static com.sap.cloud.lm.sl.cf.process.steps.StepsTestUtil.loadPlatform;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.cloudfoundry.client.lib.domain.ServiceKey;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;

import com.google.gson.reflect.TypeToken;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ApplicationsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ConfigurationEntriesCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ServiceKeysCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ServicesCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.helpers.ModuleToDeployHelper;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;
import com.sap.cloud.lm.sl.mta.handlers.v2.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.Platform;

@RunWith(Parameterized.class)
public class BuildCloudDeployModelStepTest extends SyncFlowableStepTest<BuildCloudDeployModelStep> {
    
    private static final ConfigurationParser CONFIGURATION_PARSER = new ConfigurationParser();

    private static final Integer MTA_MAJOR_SCHEMA_VERSION = 2;

    private static final DeploymentDescriptor DEPLOYMENT_DESCRIPTOR = loadDeploymentDescriptor("build-cloud-model.yaml",
        BuildCloudDeployModelStepTest.class);
    private static final Platform PLATFORM = loadPlatform(CONFIGURATION_PARSER, "platform-01.json", BuildCloudDeployModelStepTest.class);

    private static final SystemParameters EMPTY_SYSTEM_PARAMETERS = new SystemParameters(Collections.emptyMap(), Collections.emptyMap(),
        Collections.emptyMap(), Collections.emptyMap());

    protected static class StepInput {

        public String servicesToBindLocation;
        public String servicesToCreateLocation;
        public String deployedMtaLocation;
        public String serviceKeysLocation;
        public String appsToDeployLocation;
        public List<String> customDomains;

        public StepInput(String appsToDeployLocation, String servicesToBindLocation, String servicesToCreateLocation,
            String serviceKeysLocation, List<String> customDomains, String deployedMtaLocation) {
            this.servicesToBindLocation = servicesToBindLocation;
            this.servicesToCreateLocation = servicesToCreateLocation;
            this.deployedMtaLocation = deployedMtaLocation;
            this.serviceKeysLocation = serviceKeysLocation;
            this.appsToDeployLocation = appsToDeployLocation;
            this.customDomains = customDomains;
        }

    }

    protected static class StepOutput {

        public String newMtaVersion;

        public StepOutput(String newMtaVersion) {
            this.newMtaVersion = newMtaVersion;
        }

    }

    private class BuildCloudDeployModelStepMock extends BuildCloudDeployModelStep {
        @Override
        protected ApplicationsCloudModelBuilder getApplicationsCloudModelBuilder(DelegateExecution context) {
            return applicationsCloudModelBuilder;
        }

        @Override
        protected ServicesCloudModelBuilder getServicesCloudModelBuilder(DelegateExecution context) {
            return servicesCloudModelBuilder;
        }

        @Override
        protected ServiceKeysCloudModelBuilder getServiceKeysCloudModelBuilder(DelegateExecution context) {
            return serviceKeysCloudModelBuilder;
        }
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            {
                new StepInput("apps-to-deploy-05.json", "services-to-bind-01.json", "services-to-create-01.json", "service-keys-01.json", Arrays.asList("api.cf.neo.ondemand.com"), "deployed-mta-12.json"), new StepOutput("0.1.0"),
            },
            {
                new StepInput("apps-to-deploy-05.json", "services-to-bind-01.json", "services-to-create-01.json", "service-keys-01.json", Arrays.asList("api.cf.neo.ondemand.com"), null), new StepOutput("0.1.0"),
            },
            {
                new StepInput("apps-to-deploy-05.json", "services-to-bind-01.json", "services-to-create-01.json", "service-keys-01.json", Arrays.asList("api.cf.neo.ondemand.com"), null), new StepOutput("0.1.0"),
            },
// @formatter:on
        });
    }

    protected StepOutput output;
    protected StepInput input;

    protected List<CloudApplicationExtended> appsToDeploy;
    protected DeployedMta deployedMta;
    protected List<CloudServiceExtended> servicesToBind;
    protected Map<String, List<ServiceKey>> serviceKeys;

    @Mock
    protected ApplicationsCloudModelBuilder applicationsCloudModelBuilder;
    @Mock
    protected ServiceKeysCloudModelBuilder serviceKeysCloudModelBuilder;
    @Mock
    protected ServicesCloudModelBuilder servicesCloudModelBuilder;
    @Mock
    protected ConfigurationEntriesCloudModelBuilder configurationEntriesCloudModelBuilder;
    @Mock
    protected ModuleToDeployHelper moduleToDeployHelper;

    public BuildCloudDeployModelStepTest(StepInput input, StepOutput output) {
        this.output = output;
        this.input = input;
    }

    @Before
    public void setUp() throws Exception {
        loadParameters();
        prepareContext();
    }

    protected void prepareContext() throws Exception {
        context.setVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION, MTA_MAJOR_SCHEMA_VERSION);

        StepsUtil.setSystemParameters(context, EMPTY_SYSTEM_PARAMETERS);
        StepsUtil.setMtaModules(context, Collections.emptySet());
        StepsUtil.setMtaArchiveModules(context, Collections.emptySet());
        StepsUtil.setDeploymentDescriptor(context, DEPLOYMENT_DESCRIPTOR);
        StepsUtil.setXsPlaceholderReplacementValues(context, getDummyReplacementValues());

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

        assertStepFinishedSuccessfully();

        TestUtil.test(() -> StepsUtil.getServicesToBind(context), new Expectation(Expectation.Type.RESOURCE, input.servicesToBindLocation),
            getClass());
        TestUtil.test(() -> StepsUtil.getServicesToCreate(context),
            new Expectation(Expectation.Type.RESOURCE, input.servicesToCreateLocation), getClass());
        TestUtil.test(() -> StepsUtil.getServiceKeysToCreate(context),
            new Expectation(Expectation.Type.RESOURCE, input.serviceKeysLocation), getClass());
        TestUtil.test(() -> StepsUtil.getAppsToDeploy(context), new Expectation(Expectation.Type.RESOURCE, input.appsToDeployLocation),
            getClass());
        assertEquals(input.customDomains, StepsUtil.getCustomDomains(context));

        assertEquals(output.newMtaVersion, StepsUtil.getNewMtaVersion(context));
    }

    protected void loadParameters() throws Exception {
        String appsToDeployString = TestUtil.getResourceAsString(input.appsToDeployLocation, getClass());
        appsToDeploy = JsonUtil.fromJson(appsToDeployString, new TypeToken<List<CloudApplicationExtended>>() {
        }.getType());

        String servicesToBindString = TestUtil.getResourceAsString(input.servicesToBindLocation, getClass());
        servicesToBind = JsonUtil.fromJson(servicesToBindString, new TypeToken<List<CloudServiceExtended>>() {
        }.getType());

        String serviceKeysString = TestUtil.getResourceAsString(input.serviceKeysLocation, getClass());
        serviceKeys = JsonUtil.fromJson(serviceKeysString, new TypeToken<Map<String, List<ServiceKey>>>() {
        }.getType());

        if (input.deployedMtaLocation != null) {
            String deployedMtaString = TestUtil.getResourceAsString(input.deployedMtaLocation, getClass());
            deployedMta = JsonUtil.fromJson(deployedMtaString, DeployedMta.class);
        }

        when(applicationsCloudModelBuilder.build(any(), any())).thenReturn(appsToDeploy);
        when(servicesCloudModelBuilder.build(any())).thenReturn(servicesToBind);
        when(serviceKeysCloudModelBuilder.build()).thenReturn(serviceKeys);
        StepsUtil.setDeployedMta(context, deployedMta);
    }

    @Override
    protected BuildCloudDeployModelStep createStep() {
        return new BuildCloudDeployModelStepMock();
    }

}
