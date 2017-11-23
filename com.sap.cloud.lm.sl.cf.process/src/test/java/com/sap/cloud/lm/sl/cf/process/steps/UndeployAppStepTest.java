package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudInfoExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudTask;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ApplicationRoutesGetter;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.util.OneOffTasksSupportChecker;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.Pair;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class UndeployAppStepTest extends SyncActivitiStepTest<UndeployAppStep> {

    @Mock
    private OneOffTasksSupportChecker oneOffTasksSupportChecker;
    @Mock
    private ApplicationRoutesGetter applicationRoutesGetter;

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
          // (3) There are two routes that should be deleted, but one of them is bound to another application:
          {
              "undeploy-apps-step-input-03.json", "undeploy-apps-step-output-03.json",
          },
          // (4) There are running one-off tasks to cancel:
          {
              "undeploy-apps-step-input-04.json", "undeploy-apps-step-output-04.json",
          },
// @formatter:on
        });
    }

    public UndeployAppStepTest(String stepInputLocation, String stepOutputLocation) throws Exception {
        String resourceAsString = TestUtil.getResourceAsString(stepInputLocation, UndeployAppStepTest.class);
        stepInput = JsonUtil.fromJson(resourceAsString, StepInput.class);
        System.out.println(JsonUtil.toJson(stepInput, true));
        stepOutput = JsonUtil.fromJson(TestUtil.getResourceAsString(stepOutputLocation, UndeployAppStepTest.class), StepOutput.class);
    }

    @Before
    public void setUp() throws Exception {
        prepareContext();
        prepareClient();
        Mockito.when(oneOffTasksSupportChecker.areOneOffTasksSupported(client)).thenReturn(!stepInput.tasksPerApplication.isEmpty());
    }

    @Test
    public void testExecute() throws Exception {
        for (CloudApplication cloudApplication : stepInput.appsToDelete) {
            undeployApp(cloudApplication);
        }
        assertRoutesWereDeleted();
        assertTasksWereCanceled();
    }

    private void undeployApp(CloudApplication cloudApplication) throws Exception {
        step.execute(context);
        assertStepFinishedSuccessfully();
        verify(client).stopApplication(cloudApplication.getName());
        if (!cloudApplication.getUris().isEmpty()) {
            verify(client).updateApplicationUris(cloudApplication.getName(), Collections.emptyList());
        }
        verify(client).deleteApplication(cloudApplication.getName());
        StepsUtil.incrementVariable(context, Constants.VAR_APPS_TO_UNDEPLOY_INDEX);
    }

    private void assertRoutesWereDeleted() {
        int routesToDeleteCount = stepOutput.expectedRoutesToDelete.size();
        verify(clientExtensions, times(routesToDeleteCount)).deleteRoute(anyString(), anyString(), anyString());
        for (Pair<String, String> hostAndDomain : stepOutput.expectedRoutesToDelete) {
            verify(clientExtensions).deleteRoute(hostAndDomain._1, hostAndDomain._2, null);
            routesToDeleteCount--;
        }
        assertEquals("A number of routes were not deleted: ", 0, routesToDeleteCount);
    }

    private void assertTasksWereCanceled() {
        int tasksToCancelCount = stepOutput.expectedTasksToCancel.size();
        verify(clientExtensions, times(tasksToCancelCount)).cancelTask(any(UUID.class));
        for (String taskGuid : stepOutput.expectedTasksToCancel) {
            verify(clientExtensions).cancelTask(UUID.fromString(taskGuid));
            tasksToCancelCount--;
        }
        assertEquals("A number of tasks were not canceled: ", 0, tasksToCancelCount);

    }

    private void prepareContext() {
        context.setVariable(Constants.VAR_APPS_TO_UNDEPLOY_INDEX, 0);
        StepsUtil.setAppsToUndeploy(context, stepInput.appsToDelete);
    }

    private void prepareClient() {
        CloudInfoExtended info = Mockito.mock(CloudInfoExtended.class);
        Mockito.when(info.isPortBasedRouting()).thenReturn(stepInput.portBasedRouting);
        Mockito.when(client.getCloudInfo()).thenReturn(info);
        Mockito.when(applicationRoutesGetter.getRoutes(any(), anyString())).thenAnswer((invocation) -> {

            String appName = (String) invocation.getArguments()[1];
            return stepInput.appRoutesPerApplication.get(appName);

        });
        Mockito.when(clientExtensions.getTasks(anyString())).thenAnswer((invocation) -> {

            String appName = (String) invocation.getArguments()[0];
            return stepInput.tasksPerApplication.get(appName);

        });
    }

    private static class StepInput {
        private boolean portBasedRouting;
        private List<CloudApplication> appsToDelete = Collections.emptyList();
        private Map<String, List<CloudRoute>> appRoutesPerApplication = Collections.emptyMap();
        private Map<String, List<CloudTask>> tasksPerApplication = Collections.emptyMap();
    }

    private static class StepOutput {
        private List<Pair<String, String>> expectedRoutesToDelete = Collections.emptyList();
        private List<String> expectedTasksToCancel = Collections.emptyList();
    }

    @Override
    protected UndeployAppStep createStep() {
        return new UndeployAppStep();
    }

}
