package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudInfoExtended;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.Pair;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class UndeployAppsStepTest extends AbstractStepTest<UndeployAppsStep> {

    private CloudFoundryOperations client = mock(CloudFoundryOperations.class);

    private StepInput stepInput;
    private StepOutput stepOutput;

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
          // (0) There are applications to undeploy (host based routing (XSA)):
          {
              "undeploy-apps-step-input-00.json", "undeploy-apps-step-output-00.json",
          },
          // (1) There are applications to undeploy (port based routing (XSA)):
          {
              "undeploy-apps-step-input-01.json", "undeploy-apps-step-output-01.json",
          },
          // (2) No applications to undeploy:
          {
              "undeploy-apps-step-input-02.json", "undeploy-apps-step-output-02.json",
          },
// @formatter:on
        });
    }

    public UndeployAppsStepTest(String stepInputLocation, String stepOutputLocation) throws Exception {
        stepInput = JsonUtil.fromJson(TestUtil.getResourceAsString(stepInputLocation, UndeployAppsStepTest.class), StepInput.class);
        stepOutput = JsonUtil.fromJson(TestUtil.getResourceAsString(stepOutputLocation, UndeployAppsStepTest.class), StepOutput.class);
    }

    @Before
    public void setUp() throws Exception {
        prepareContext();
        prepareClient();
        step.clientSupplier = (context) -> client;
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertEquals(ExecutionStatus.SUCCESS.toString(),
            context.getVariable(com.sap.activiti.common.Constants.STEP_NAME_PREFIX + step.getLogicalStepName()));

        for (CloudApplication cloudApplication : stepInput.appsToDelete) {
            verify(client, times(1)).stopApplication(cloudApplication.getName());
            verify(client, times(1)).updateApplicationUris(cloudApplication.getName(), Collections.emptyList());
            verify(client, times(1)).deleteApplication(cloudApplication.getName());
        }
        int routesToDeleteCount = stepOutput.expectedRoutesToDelete.size();
        for (Pair<String, String> hostAndDomain : stepOutput.expectedRoutesToDelete) {
            verify(client, times(1)).deleteRoute(hostAndDomain._1, hostAndDomain._2);
            routesToDeleteCount--;
        }
        assertEquals("A number of routes were not called for deletion: ", 0, routesToDeleteCount);
    }

    private void prepareContext() {
        StepsUtil.setAppsToUndeploy(context, stepInput.appsToDelete);
    }

    private void prepareClient() {
        CloudInfoExtended info = Mockito.mock(CloudInfoExtended.class);
        Mockito.when(info.isPortBasedRouting()).thenReturn(stepInput.portBasedRouting);
        Mockito.when(client.getCloudInfo()).thenReturn(info);
    }

    private static class StepInput {
        private boolean portBasedRouting;
        private List<CloudApplication> appsToDelete;
    }

    private static class StepOutput {
        private List<Pair<String, String>> expectedRoutesToDelete;
    }

    @Override
    protected UndeployAppsStep createStep() {
        return new UndeployAppsStep();
    }

}
