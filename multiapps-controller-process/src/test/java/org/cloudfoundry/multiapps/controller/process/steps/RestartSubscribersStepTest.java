package org.cloudfoundry.multiapps.controller.process.steps;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudSpace;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudOrganization;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudSpace;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;

class RestartSubscribersStepTest extends SyncFlowableStepTest<RestartSubscribersStep> {

    private final UUID FOO_SPACE_GUID = UUID.randomUUID();
    private final UUID BAR_SPACE_GUID = UUID.randomUUID();

    @Test
    void testClientsForCorrectSpacesAreRequested() {
        // Given:
        List<CloudApplication> updatedSubscribers = new ArrayList<>();
        updatedSubscribers.add(createCloudApplication("app", createCloudSpace(FOO_SPACE_GUID)));
        updatedSubscribers.add(createCloudApplication("app", createCloudSpace(BAR_SPACE_GUID)));
        context.setVariable(Variables.UPDATED_SUBSCRIBERS, updatedSubscribers);

        // When:
        step.execute(execution);

        // Then:
        Mockito.verify(clientProvider, Mockito.atLeastOnce())
               .getControllerClient(eq(USER_NAME), eq(FOO_SPACE_GUID.toString()), anyString());
        Mockito.verify(clientProvider, Mockito.atLeastOnce())
               .getControllerClient(eq(USER_NAME), eq(BAR_SPACE_GUID.toString()), anyString());
    }

    @Test
    void testSubscribersAreRestartedWhenClientExtensionsAreNotSupported() {
        // Given:
        setupSubscribers();

        CloudControllerClient clientForSpaceFoo = Mockito.mock(CloudControllerClient.class);
        CloudControllerClient clientForSpaceBar = Mockito.mock(CloudControllerClient.class);
        Mockito.when(clientProvider.getControllerClient(eq(USER_NAME), eq(FOO_SPACE_GUID.toString()), anyString()))
               .thenReturn(clientForSpaceFoo);
        Mockito.when(clientProvider.getControllerClient(eq(USER_NAME), eq(BAR_SPACE_GUID.toString()), anyString()))
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
        setupSubscribers();

        CloudControllerClient clientForSpaceFoo = Mockito.mock(CloudControllerClient.class);
        CloudControllerClient clientForSpaceBar = Mockito.mock(CloudControllerClient.class);

        Mockito.when(clientProvider.getControllerClient(eq(USER_NAME), eq(FOO_SPACE_GUID.toString()), anyString()))
               .thenReturn(clientForSpaceFoo);
        Mockito.when(clientProvider.getControllerClient(eq(USER_NAME), eq(BAR_SPACE_GUID.toString()), anyString()))
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
        setupSubscribers();

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

    private void setupSubscribers() {
        List<CloudApplication> updatedSubscribers = new ArrayList<>();
        updatedSubscribers.add(createCloudApplication("app-1", createCloudSpace(FOO_SPACE_GUID)));
        updatedSubscribers.add(createCloudApplication("app-2", createCloudSpace(BAR_SPACE_GUID)));
        context.setVariable(Variables.UPDATED_SUBSCRIBERS, updatedSubscribers);
    }

    private CloudApplication createCloudApplication(String appName, CloudSpace space) {
        return ImmutableCloudApplicationExtended.builder()
                                                .name(appName)
                                                .space(space)
                                                .build();
    }

    private CloudSpace createCloudSpace(UUID guid) {
        return ImmutableCloudSpace.builder()
                                  .metadata(ImmutableCloudMetadata.of(guid))
                                  .organization(ImmutableCloudOrganization.builder()
                                                                          .name("orgName")
                                                                          .build())
                                  .name("spaceName")
                                  .build();
    }

    @Override
    protected RestartSubscribersStep createStep() {
        return new RestartSubscribersStep();
    }

}
