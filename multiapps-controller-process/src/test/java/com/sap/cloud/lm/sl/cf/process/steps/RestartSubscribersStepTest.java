package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.client.XsCloudControllerClient;

public class RestartSubscribersStepTest extends SyncFlowableStepTest<RestartSubscribersStep> {

    @Test
    public void testClientsForCorrectSpacesAreRequested() throws Exception {
        // Given:
        List<CloudApplication> updatedSubscribers = new ArrayList<>();
        updatedSubscribers.add(createCloudApplication("app", createCloudSpace("org", "space-foo")));
        updatedSubscribers.add(createCloudApplication("app", createCloudSpace("org", "space-bar")));
        StepsUtil.setUpdatedSubscribers(context, updatedSubscribers);

        // When:
        step.execute(context);

        // Then:
        Mockito.verify(clientProvider, Mockito.atLeastOnce())
               .getControllerClient(eq(USER_NAME), eq("org"), eq("space-foo"), anyString());
        Mockito.verify(clientProvider, Mockito.atLeastOnce())
               .getControllerClient(eq(USER_NAME), eq("org"), eq("space-bar"), anyString());
    }

    @Test
    public void testSubscribersAreRestartedWhenClientExtensionsAreNotSupported() throws Exception {
        // Given:
        List<CloudApplication> updatedSubscribers = new ArrayList<>();
        updatedSubscribers.add(createCloudApplication("app-1", createCloudSpace("org", "space-foo")));
        updatedSubscribers.add(createCloudApplication("app-2", createCloudSpace("org", "space-bar")));
        StepsUtil.setUpdatedSubscribers(context, updatedSubscribers);

        CloudControllerClient clientForSpaceFoo = Mockito.mock(CloudControllerClient.class);
        CloudControllerClient clientForSpaceBar = Mockito.mock(CloudControllerClient.class);
        Mockito.when(clientProvider.getControllerClient(eq(USER_NAME), eq("org"), eq("space-foo"), anyString()))
               .thenReturn(clientForSpaceFoo);
        Mockito.when(clientProvider.getControllerClient(eq(USER_NAME), eq("org"), eq("space-bar"), anyString()))
               .thenReturn(clientForSpaceBar);

        // When:
        step.execute(context);

        // Then:
        assertStepFinishedSuccessfully();
        Mockito.verify(clientForSpaceFoo)
               .stopApplication("app-1");
        Mockito.verify(clientForSpaceFoo)
               .startApplication("app-1");
        Mockito.verify(clientForSpaceBar)
               .stopApplication("app-2");
        Mockito.verify(clientForSpaceBar)
               .startApplication("app-2");
    }

    @Test
    public void testSubscribersAreRestartedWhenClientExtensionsAreSupported() throws Exception {
        // Given:
        List<CloudApplication> updatedSubscribers = new ArrayList<>();
        updatedSubscribers.add(createCloudApplication("app-1", createCloudSpace("org", "space-foo")));
        updatedSubscribers.add(createCloudApplication("app-2", createCloudSpace("org", "space-bar")));
        StepsUtil.setUpdatedSubscribers(context, updatedSubscribers);

        XsCloudControllerClient clientForSpaceFoo = Mockito.mock(XsCloudControllerClient.class);
        XsCloudControllerClient clientForSpaceBar = Mockito.mock(XsCloudControllerClient.class);

        Mockito.when(clientProvider.getControllerClient(eq(USER_NAME), eq("org"), eq("space-foo"), anyString()))
               .thenReturn(clientForSpaceFoo);
        Mockito.when(clientProvider.getControllerClient(eq(USER_NAME), eq("org"), eq("space-bar"), anyString()))
               .thenReturn(clientForSpaceBar);

        // When:
        step.execute(context);

        // Then:
        assertStepFinishedSuccessfully();
        Mockito.verify(clientForSpaceFoo)
               .stopApplication("app-1");
        Mockito.verify(clientForSpaceFoo)
               .startApplication("app-1", false);
        Mockito.verify(clientForSpaceBar)
               .stopApplication("app-2");
        Mockito.verify(clientForSpaceBar)
               .startApplication("app-2", false);
    }

    @Test
    public void testNothingHappensWhenThereAreNoSubscribersToRestart() throws Exception {
        // Given:
        StepsUtil.setUpdatedSubscribers(context, Collections.emptyList());

        // When:
        step.execute(context);

        // Then:
        assertStepFinishedSuccessfully();
    }

    @Test
    public void testOtherSubscribersAreRestartedWhenOneRestartFails() throws Exception {
        // Given:
        List<CloudApplication> updatedSubscribers = new ArrayList<>();
        updatedSubscribers.add(createCloudApplication("app-1", createCloudSpace(ORG_NAME, SPACE_NAME)));
        updatedSubscribers.add(createCloudApplication("app-2", createCloudSpace(ORG_NAME, SPACE_NAME)));
        StepsUtil.setUpdatedSubscribers(context, updatedSubscribers);

        Mockito.doThrow(new CloudOperationException(HttpStatus.INTERNAL_SERVER_ERROR))
               .when(client)
               .stopApplication("app-1");

        // When:
        step.execute(context);

        // Then:
        assertStepFinishedSuccessfully();
        Mockito.verify(client)
               .stopApplication("app-1");
        Mockito.verify(client)
               .stopApplication("app-2");
        Mockito.verify(client)
               .startApplication("app-2", false);
    }

    private CloudApplication createCloudApplication(String appName, CloudSpace space) {
        CloudApplication app = new CloudApplication(null, appName);
        app.setSpace(space);
        return app;
    }

    private CloudSpace createCloudSpace(String orgName, String spaceName) {
        CloudOrganization org = new CloudOrganization(null, orgName);
        return new CloudSpace(null, spaceName, org);
    }

    @Override
    protected RestartSubscribersStep createStep() {
        return new RestartSubscribersStep();
    }

}
