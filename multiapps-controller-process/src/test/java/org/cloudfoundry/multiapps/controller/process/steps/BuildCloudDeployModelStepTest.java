package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.client.lib.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.common.util.TestUtil;
import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.cf.util.ModulesCloudModelBuilderContentCalculator;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ApplicationCloudModelBuilder;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ConfigurationEntriesCloudModelBuilder;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ServiceKeysCloudModelBuilder;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ServicesCloudModelBuilder;
import org.cloudfoundry.multiapps.controller.core.helpers.ModuleToDeployHelper;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.util.DescriptorTestUtil;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;

import com.fasterxml.jackson.core.type.TypeReference;

@RunWith(Parameterized.class)
public class BuildCloudDeployModelStepTest extends SyncFlowableStepTest<BuildCloudDeployModelStep> {

    private static final Integer MTA_MAJOR_SCHEMA_VERSION = 2;

    private static final DeploymentDescriptor DEPLOYMENT_DESCRIPTOR = DescriptorTestUtil.loadDeploymentDescriptor("build-cloud-model.yaml",
                                                                                                                  BuildCloudDeployModelStepTest.class);

    protected static class StepInput {

        public final String servicesToBindLocation;
        public final String servicesToCreateLocation;
        public final String deployedMtaLocation;
        public final String serviceKeysLocation;
        public final String modulesToDeployLocation;
        public final List<String> customDomains;

        public StepInput(String modulesToDeployLocation, String servicesToBindLocation, String servicesToCreateLocation,
                         String serviceKeysLocation, List<String> customDomains, String deployedMtaLocation) {
            this.servicesToBindLocation = servicesToBindLocation;
            this.servicesToCreateLocation = servicesToCreateLocation;
            this.deployedMtaLocation = deployedMtaLocation;
            this.serviceKeysLocation = serviceKeysLocation;
            this.modulesToDeployLocation = modulesToDeployLocation;
            this.customDomains = customDomains;
        }

    }

    protected static class StepOutput {

        public final String newMtaVersion;

        public StepOutput(String newMtaVersion) {
            this.newMtaVersion = newMtaVersion;
        }

    }

    private class BuildCloudDeployModelStepMock extends BuildCloudDeployModelStep {
        @Override
        protected ApplicationCloudModelBuilder getApplicationCloudModelBuilder(ProcessContext context) {
            return applicationCloudModelBuilder;
        }

        @Override
        protected ModulesCloudModelBuilderContentCalculator
                  getModulesContentCalculator(ProcessContext context, Set<String> mtaArchiveModules, Set<String> deployedModuleNames,
                                              Set<String> allMtaModules) {
            return modulesCloudModelBuilderContentCalculator;
        }

        @Override
        protected ServicesCloudModelBuilder getServicesCloudModelBuilder(ProcessContext context) {
            return servicesCloudModelBuilder;
        }

        @Override
        protected ServiceKeysCloudModelBuilder getServiceKeysCloudModelBuilder(ProcessContext context) {
            return serviceKeysCloudModelBuilder;
        }
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            {
                new StepInput("modules-to-deploy-01.json", "services-to-bind-01.json", "services-to-create-01.json", "service-keys-01.json", Collections.singletonList("api.cf.neo.ondemand.com"), "deployed-mta-12.json"), new StepOutput("0.1.0"),
            },
            {
                new StepInput("modules-to-deploy-01.json", "services-to-bind-01.json", "services-to-create-01.json", "service-keys-01.json", Collections.singletonList("api.cf.neo.ondemand.com"), null), new StepOutput("0.1.0"),
            },
// @formatter:on
        });
    }

    protected final StepOutput output;
    protected final StepInput input;

    protected List<Module> modulesToDeploy;
    protected DeployedMta deployedMta;
    protected List<CloudServiceInstanceExtended> servicesToBind;
    protected Map<String, List<CloudServiceKey>> serviceKeys;

    @Mock
    protected ApplicationCloudModelBuilder applicationCloudModelBuilder;
    @Mock
    protected ModulesCloudModelBuilderContentCalculator modulesCloudModelBuilderContentCalculator;
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

    protected void prepareContext() {
        context.setVariable(Variables.MTA_MAJOR_SCHEMA_VERSION, MTA_MAJOR_SCHEMA_VERSION);

        context.setVariable(Variables.MTA_MODULES, Collections.emptySet());
        context.setVariable(Variables.MTA_ARCHIVE_MODULES, Collections.emptySet());
        context.setVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR, DEPLOYMENT_DESCRIPTOR);
    }

    @Test
    public void testExecute() {
        step.execute(execution);

        assertStepFinishedSuccessfully();

        tester.test(() -> context.getVariable(Variables.SERVICES_TO_BIND),
                    new Expectation(Expectation.Type.JSON, input.servicesToBindLocation));
        tester.test(() -> context.getVariable(Variables.SERVICES_TO_CREATE),
                    new Expectation(Expectation.Type.JSON, input.servicesToCreateLocation));
        tester.test(() -> context.getVariable(Variables.SERVICE_KEYS_TO_CREATE),
                    new Expectation(Expectation.Type.JSON, input.serviceKeysLocation));
        tester.test(() -> context.getVariable(Variables.MODULES_TO_DEPLOY),
                    new Expectation(Expectation.Type.JSON, input.modulesToDeployLocation));
        tester.test(() -> context.getVariable(Variables.ALL_MODULES_TO_DEPLOY),
                    new Expectation(Expectation.Type.JSON, input.modulesToDeployLocation));
        assertFalse(context.getVariable(Variables.USE_IDLE_URIS));
        assertEquals(input.customDomains, context.getVariable(Variables.CUSTOM_DOMAINS));
    }

    protected void loadParameters() {
        String modulesToDeployString = TestUtil.getResourceAsString(input.modulesToDeployLocation, getClass());
        modulesToDeploy = JsonUtil.fromJson(modulesToDeployString, new TypeReference<List<Module>>() {
        });

        String servicesToBindString = TestUtil.getResourceAsString(input.servicesToBindLocation, getClass());
        servicesToBind = JsonUtil.fromJson(servicesToBindString, new TypeReference<List<CloudServiceInstanceExtended>>() {
        });

        String serviceKeysString = TestUtil.getResourceAsString(input.serviceKeysLocation, getClass());
        serviceKeys = JsonUtil.fromJson(serviceKeysString, new TypeReference<Map<String, List<CloudServiceKey>>>() {
        });

        if (input.deployedMtaLocation != null) {
            String deployedMtaString = TestUtil.getResourceAsString(input.deployedMtaLocation, getClass());
            deployedMta = JsonUtil.fromJson(deployedMtaString, DeployedMta.class);
        }
        when(moduleToDeployHelper.isApplication(any())).thenReturn(true);
        when(modulesCloudModelBuilderContentCalculator.calculateContentForBuilding(any())).thenReturn(modulesToDeploy);
        when(applicationCloudModelBuilder.getApplicationDomains(any(), any())).thenReturn(input.customDomains);
        when(servicesCloudModelBuilder.build(any())).thenReturn(servicesToBind);
        when(serviceKeysCloudModelBuilder.build()).thenReturn(serviceKeys);
        context.setVariable(Variables.DEPLOYED_MTA, deployedMta);
    }

    @Override
    protected BuildCloudDeployModelStep createStep() {
        return new BuildCloudDeployModelStepMock();
    }

}
