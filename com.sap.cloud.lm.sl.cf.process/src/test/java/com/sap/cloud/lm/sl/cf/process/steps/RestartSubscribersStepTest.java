package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.ImmutableCloudOrganization;
import org.cloudfoundry.client.lib.domain.ImmutableCloudSpace;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudApplicationExtended;

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
               .startApplication("app-2");
    }

    private CloudApplication createCloudApplication(String appName, CloudSpace space) {
        return ImmutableCloudApplicationExtended.builder()
                                                .name(appName)
                                                .space(space)
                                                .build();
    }

    private CloudSpace createCloudSpace(String orgName, String spaceName) {
        return ImmutableCloudSpace.builder()
                                  .organization(ImmutableCloudOrganization.builder()
                                                                          .name(orgName)
                                                                          .build())
                                  .name(spaceName)
                                  .build();
    }

    @Override
    protected RestartSubscribersStep createStep() {
        return new RestartSubscribersStep();
    }

}
