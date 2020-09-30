package org.cloudfoundry.multiapps.controller.process.steps;

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
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

class RestartSubscribersStepTest extends SyncFlowableStepTest<RestartSubscribersStep> {

    @Test
    void testClientsForCorrectSpacesAreRequested() {
        // Given:
        List<CloudApplication> updatedSubscribers = new ArrayList<>();
        updatedSubscribers.add(createCloudApplication("app", createCloudSpace("org", "space-foo")));
        updatedSubscribers.add(createCloudApplication("app", createCloudSpace("org", "space-bar")));
        context.setVariable(Variables.UPDATED_SUBSCRIBERS, updatedSubscribers);

        // When:
        step.execute(execution);

        // Then:
        Mockito.verify(clientProvider, Mockito.atLeastOnce())
               .getControllerClient(eq(USER_NAME), eq("org"), eq("space-foo"), anyString());
        Mockito.verify(clientProvider, Mockito.atLeastOnce())
               .getControllerClient(eq(USER_NAME), eq("org"), eq("space-bar"), anyString());
    }

    @Test
    void testSubscribersAreRestartedWhenClientExtensionsAreNotSupported() {
        // Given:
        List<CloudApplication> updatedSubscribers = new ArrayList<>();
        updatedSubscribers.add(createCloudApplication("app-1", createCloudSpace("org", "space-foo")));
        updatedSubscribers.add(createCloudApplication("app-2", createCloudSpace("org", "space-bar")));
        context.setVariable(Variables.UPDATED_SUBSCRIBERS, updatedSubscribers);

        CloudControllerClient clientForSpaceFoo = Mockito.mock(CloudControllerClient.class);
        CloudControllerClient clientForSpaceBar = Mockito.mock(CloudControllerClient.class);
        Mockito.when(clientProvider.getControllerClient(eq(USER_NAME), eq("org"), eq("space-foo"), anyString()))
               .thenReturn(clientForSpaceFoo);
        Mockito.when(clientProvider.getControllerClient(eq(USER_NAME), eq("org"), eq("space-bar"), anyString()))
               .thenReturn(clientForSpaceBar);

        // When:
        step.execute(execution);

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
    void testSubscribersAreRestartedWhenClientExtensionsAreSupported() {
        // Given:
        List<CloudApplication> updatedSubscribers = new ArrayList<>();
        updatedSubscribers.add(createCloudApplication("app-1", createCloudSpace("org", "space-foo")));
        updatedSubscribers.add(createCloudApplication("app-2", createCloudSpace("org", "space-bar")));
        context.setVariable(Variables.UPDATED_SUBSCRIBERS, updatedSubscribers);

        CloudControllerClient clientForSpaceFoo = Mockito.mock(CloudControllerClient.class);
        CloudControllerClient clientForSpaceBar = Mockito.mock(CloudControllerClient.class);

        Mockito.when(clientProvider.getControllerClient(eq(USER_NAME), eq("org"), eq("space-foo"), anyString()))
               .thenReturn(clientForSpaceFoo);
        Mockito.when(clientProvider.getControllerClient(eq(USER_NAME), eq("org"), eq("space-bar"), anyString()))
               .thenReturn(clientForSpaceBar);

        // When:
        step.execute(execution);

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
    void testNothingHappensWhenThereAreNoSubscribersToRestart() {
        // Given:
        context.setVariable(Variables.UPDATED_SUBSCRIBERS, Collections.emptyList());

        // When:
        step.execute(execution);

        // Then:
        assertStepFinishedSuccessfully();
    }

    @Test
    void testOtherSubscribersAreRestartedWhenOneRestartFails() {
        // Given:
        List<CloudApplication> updatedSubscribers = new ArrayList<>();
        updatedSubscribers.add(createCloudApplication("app-1", createCloudSpace(ORG_NAME, SPACE_NAME)));
        updatedSubscribers.add(createCloudApplication("app-2", createCloudSpace(ORG_NAME, SPACE_NAME)));
        context.setVariable(Variables.UPDATED_SUBSCRIBERS, updatedSubscribers);

        Mockito.doThrow(new CloudOperationException(HttpStatus.INTERNAL_SERVER_ERROR))
               .when(client)
               .stopApplication("app-1");

        // When:
        step.execute(execution);

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
