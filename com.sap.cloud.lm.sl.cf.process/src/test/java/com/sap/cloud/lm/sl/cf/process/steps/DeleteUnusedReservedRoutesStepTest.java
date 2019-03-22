package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.flowable.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;

import com.sap.cloud.lm.sl.cf.core.cf.v2.ApplicationCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.helpers.ModuleToDeployHelper;
import com.sap.cloud.lm.sl.cf.core.helpers.PortAllocator;
import com.sap.cloud.lm.sl.cf.core.helpers.PortAllocatorMock;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.PortValidator;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.mta.model.v2.Module;

@RunWith(Parameterized.class)
public class DeleteUnusedReservedRoutesStepTest extends SyncFlowableStepTest<DeleteUnusedReservedRoutesStep> {

    private static final String DEFAULT_DOMAIN = "localhost";

    private static class StepInput {
        Map<String, Set<Integer>> allocatedPorts;
        public boolean portBasedRouting;
        private List<Module> modulesToDeploy;
        public Map<String, List<String>> modulesUrls;
    }

    private static class StepOutput {
        public Map<String, Set<Integer>> allocatedPorts;
    }

    private class DeleteUnusedReservedRoutesStepMock extends DeleteUnusedReservedRoutesStep {
        @Override
        protected ApplicationCloudModelBuilder getApplicationCloudModelBuilder(DelegateExecution context) {
            return applicationCloudModelBuilder;
        }
        
        @Override
        protected List<Module> getModulesToDeploy(DelegateExecution context) {
            return input.modulesToDeploy;
        }
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) There aren't any unused ports:
            {
                "delete-unused-reserved-routes-input-1.json", "delete-unused-reserved-routes-output-1.json"
            },
            // (1) There are unused ports:
            {
                "delete-unused-reserved-routes-input-2.json" , "delete-unused-reserved-routes-output-1.json"
            },
            // (2) Host based routing:
            {
                "delete-unused-reserved-routes-input-3.json" , "delete-unused-reserved-routes-output-2.json"
            },
// @formatter:on
        });
    }

    private StepOutput output;
    private StepInput input;

    private PortAllocator portAllocator = new PortAllocatorMock(PortValidator.MIN_PORT_VALUE);

    @Mock
    protected ApplicationCloudModelBuilder applicationCloudModelBuilder;
    @Mock
    protected ModuleToDeployHelper moduleToDeployHelper;

    public DeleteUnusedReservedRoutesStepTest(String stepInput, String stepOutput) {
        this.input = JsonUtil.fromJson(TestUtil.getResourceAsString(stepInput, DeleteUnusedReservedRoutesStepTest.class), StepInput.class);
        this.output = JsonUtil.fromJson(TestUtil.getResourceAsString(stepOutput, DeleteUnusedReservedRoutesStepTest.class),
            StepOutput.class);
    }

    @Before
    public void setUp() throws Exception {
        loadParameters();
        prepareContext();
        prepareClient();
    }

    private void prepareClient() throws Exception {
        when(clientProvider.getPortAllocator(any(), anyString())).thenReturn(portAllocator);
    }

    private void loadParameters() throws Exception {
        when(moduleToDeployHelper.isApplication(any())).thenReturn(true);
        for (Module module : input.modulesToDeploy) {
            when(applicationCloudModelBuilder.getApplicationUris(module)).thenReturn(input.modulesUrls.get(module.getName()));
        }
    }

    private void prepareContext() throws Exception {
        StepsUtil.setXsPlaceholderReplacementValues(context, getReplacementValues());
        StepsUtil.setModulesToDeploy(context, input.modulesToDeploy);
        StepsUtil.setMtaModules(context, input.modulesToDeploy.stream()
            .map(Module::getName)
            .collect(Collectors.toSet()));
        context.setVariable(Constants.VAR_PORT_BASED_ROUTING, input.portBasedRouting);
        StepsUtil.setAllocatedPorts(context, input.allocatedPorts);
    }

    private Map<String, Object> getReplacementValues() {
        Map<String, Object> result = new TreeMap<>();
        result.put(SupportedParameters.XSA_DEFAULT_DOMAIN_PLACEHOLDER, DEFAULT_DOMAIN);
        return result;
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();

        assertEquals(output.allocatedPorts, StepsUtil.getAllocatedPorts(context));
    }

    @Override
    protected DeleteUnusedReservedRoutesStep createStep() {
        return new DeleteUnusedReservedRoutesStepMock();
    }

}
