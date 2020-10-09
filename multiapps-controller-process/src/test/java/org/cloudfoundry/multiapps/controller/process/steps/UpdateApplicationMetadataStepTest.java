package org.cloudfoundry.multiapps.controller.process.steps;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class UpdateApplicationMetadataStepTest extends SyncFlowableStepTest<UpdateApplicationMetadataStep> {

    private static final String APP_NAME = "test_application";
    private static final UUID APP_GUID = UUID.randomUUID();

    static Stream<Arguments> testStep() {
        return Stream.of(
                         // (1) Update metadata of new application
                         Arguments.of(Metadata.builder()
                                              .label("test-label", "test-value")
                                              .build(),
                                      null, true, true),
                         // (2) Update metadata of existing application
                         Arguments.of(Metadata.builder()
                                              .annotation("test-annotation", "new-value")
                                              .build(),
                                      Metadata.builder()
                                              .annotation("test-annotation", "old-value")
                                              .build(),
                                      false, true),
                         // (3) Do not update equal metadata
                         Arguments.of(Metadata.builder()
                                              .label("test-label", "test-value")
                                              .build(),
                                      Metadata.builder()
                                              .label("test-label", "test-value")
                                              .build(),
                                      false, false),
                         // (4) Update metadata of existing application with null metadata
                         Arguments.of(Metadata.builder()
                                              .label("test-label", "test-value")
                                              .build(),
                                      null, false, true),
                         // (5) Do not update existing metadata with null
                         Arguments.of(null, Metadata.builder()
                                                    .label("test-label", "test-value")
                                                    .build(),
                                      false, false));
    }

    @ParameterizedTest
    @MethodSource
    void testStep(Metadata deploymentMetadata, Metadata existingDeploymentMetadata, boolean newApplication,
                  boolean expectedMetadataUpdate) {
        prepareContext(deploymentMetadata, existingDeploymentMetadata, newApplication);

        step.execute(execution);

        assertStepFinishedSuccessfully();
        if (expectedMetadataUpdate) {
            verify(client).updateApplicationMetadata(APP_GUID, deploymentMetadata);
            return;
        }
        verify(client, never()).updateApplicationMetadata(APP_GUID, deploymentMetadata);
    }

    private void prepareContext(Metadata deploymentMetadata, Metadata existingDeploymentMetadata, boolean newApplication) {
        CloudApplicationExtended application = buildApplication(deploymentMetadata);
        CloudApplication existingApplication = buildApplication(existingDeploymentMetadata);
        context.setVariable(Variables.APP_TO_PROCESS, application);
        context.setVariable(Variables.EXISTING_APP, newApplication ? null : existingApplication);
        when(client.getApplication(APP_NAME)).thenReturn(existingApplication);
    }

    private CloudApplicationExtended buildApplication(Metadata v3Metadata) {
        return ImmutableCloudApplicationExtended.builder()
                                                .name(APP_NAME)
                                                .metadata(ImmutableCloudMetadata.builder()
                                                                                .guid(APP_GUID)
                                                                                .build())
                                                .v3Metadata(v3Metadata)
                                                .build();
    }

    @Override
    protected UpdateApplicationMetadataStep createStep() {
        return new UpdateApplicationMetadataStep();
    }

}
