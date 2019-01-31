package com.sap.cloud.lm.sl.cf.process.steps;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.client.lib.domain.ApplicationPort;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ApplicationPort.ApplicationPortType;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class ReserveRoutesStepTest extends SyncFlowableStepTest<ReserveRoutesStep> {

    private StepInput stepInput;

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) No default allocated ports
            {
                "reserve-routes-step-input-1.json",
            },
            // (1) Some default allocated ports
            {
                "reserve-routes-step-input-2.json",
            }
// @formatter:on
        });
    }

    public ReserveRoutesStepTest(String stepInput) throws IOException, ParsingException {
        this.stepInput = JsonUtil.fromJson(TestUtil.getResourceAsString(stepInput, ReserveRoutesStepTest.class), StepInput.class);
    }

    @Before
    public void setUp() throws Exception {
        prepareContext();
    }

    private void prepareContext() {
        context.setVariable(Constants.VAR_PORT_BASED_ROUTING, true);
        StepsUtil.setAllocatedPorts(context, stepInput.allocatedPorts);
        CloudApplicationExtended app = new CloudApplicationExtended(null, "appName");
        app.setModuleName("appName");
        app.setApplicationPorts(stepInput.applicationPorts);
        app.setDomains(stepInput.domains);
        StepsUtil.setApp(context, app);
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();
        verifyClientInteraction();
    }

    private void verifyClientInteraction() {
        for (ApplicationPort applicationPort : stepInput.applicationPorts) {
            if (shouldHaveReservedTcpPort(applicationPort)) {
                verifyReservedForDomains(applicationPort);
            }
        }
    }

    private void verifyReservedForDomains(ApplicationPort applicationPort) {
        for (String domain : stepInput.domains) {
            boolean tcps = ApplicationPortType.TCPS.equals(applicationPort.getPortType());
            Mockito.verify(client)
                .reserveTcpPort(applicationPort.getPort(), domain, tcps);
        }
    }

    private boolean shouldHaveReservedTcpPort(ApplicationPort applicationPort) {
        return !ApplicationPortType.HTTP.equals(applicationPort.getPortType())
            && !getAllAllocatedPorts().contains(applicationPort.getPort());
    }

    private Set<Integer> getAllAllocatedPorts() {
        Set<Integer> allPorts = new TreeSet<>();
        for (String module : stepInput.allocatedPorts.keySet()) {
            allPorts.addAll(stepInput.allocatedPorts.get(module));
        }
        return allPorts;
    }

    @Override
    protected ReserveRoutesStep createStep() {
        return new ReserveRoutesStep();
    }

    private static class StepInput {
        List<String> domains;
        List<ApplicationPort> applicationPorts;
        Map<String, Set<Integer>> allocatedPorts;
    }
}
