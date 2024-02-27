package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.gson.reflect.TypeToken;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.helpers.PortAllocator;
import com.sap.cloud.lm.sl.cf.core.helpers.PortAllocatorMock;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.PortValidator;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class DeleteUnusedReservedRoutesStepTest extends SyncFlowableStepTest<DeleteUnusedReservedRoutesStep> {

    private static final String DEFAULT_DOMAIN = "localhost";
    private List<CloudApplicationExtended> appsToDeploy;
    private StepOutput output;
    private StepInput input;
    private PortAllocator portAllocator = new PortAllocatorMock(PortValidator.MIN_PORT_VALUE);

    public DeleteUnusedReservedRoutesStepTest(StepInput input, StepOutput output) {
        this.output = output;
        this.input = input;
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) There aren't any unused ports:
            {
                new StepInput("apps-to-deploy-02.json", new TreeSet<>(Arrays.asList(10002, 10003)), true), new StepOutput(new TreeSet<>(Arrays.asList(10002, 10003))),
            },
            // (1) There are unused ports:
            {
                new StepInput("apps-to-deploy-02.json", new TreeSet<>(Arrays.asList(10001, 10002, 10003)), true), new StepOutput(new TreeSet<>(Arrays.asList(10002, 10003))),
            },
            // (2) Host based routing:
            {
                new StepInput("apps-to-deploy-02.json", Collections.emptySet(), false), new StepOutput(Collections.emptySet()),
            },
// @formatter:on
        });
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
        String appsToDeployString = TestUtil.getResourceAsString(input.appsToDeployLocation, getClass());

        appsToDeploy = JsonUtil.fromJson(appsToDeployString, new TypeToken<List<CloudApplicationExtended>>() {
        }.getType());
    }

    private void prepareContext() throws Exception {
        StepsUtil.setXsPlaceholderReplacementValues(context, getReplacementValues());

        context.setVariable(Constants.VAR_PORT_BASED_ROUTING, input.portBasedRouting);
        StepsUtil.setAllocatedPorts(context, input.allocatedPorts);
        StepsUtil.setAppsToDeploy(context, appsToDeploy);
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
        return new DeleteUnusedReservedRoutesStep();
    }

    private static class StepInput {

        public String appsToDeployLocation;
        public Set<Integer> allocatedPorts;
        public boolean portBasedRouting;

        public StepInput(String appsToDeployLocation, Set<Integer> allocatedPorts, boolean portBasedRouting) {
            this.appsToDeployLocation = appsToDeployLocation;
            this.allocatedPorts = allocatedPorts;
            this.portBasedRouting = portBasedRouting;
        }

    }

    private static class StepOutput {

        public Set<Integer> allocatedPorts;

        public StepOutput(Set<Integer> allocatedPorts) {
            this.allocatedPorts = allocatedPorts;
        }

    }

}
