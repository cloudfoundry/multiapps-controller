package org.cloudfoundry.multiapps.controller.process.steps;

import static org.mockito.Mockito.verify;

import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataAnnotations;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataLabels;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.util.MtaMetadataUtil;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;

class PrepareApplicationForBackupStepTest extends SyncFlowableStepTest<PrepareApplicationForBackupStep> {

    private static Stream<Arguments> testStep() {
        return Stream.of(
                         // (1) Rename standard app name with system namespace
                         Arguments.of("test-app", null, "mta-backup-test-app", "mta-backup"),
                         // (2) Rename app name with non idle suffix and add system namespace prefix
                         Arguments.of("test-app-idle", null, "mta-backup-test-app", "mta-backup"),
                         // (3) Reanme app name with blue suffix without any blue/green suffixes and add system namespace
                         Arguments.of("test-app-blue", null, "mta-backup-test-app", "mta-backup"),
                         // (4) Reanme app name with green suffix without any blue/green suffixes and add system namespace
                         Arguments.of("test-app-green", null, "mta-backup-test-app", "mta-backup"),
                         // (5) Rename app with namespace prefix to system+user namespace
                         Arguments.of("dev-web-app", "dev", "mta-backup-dev-web-app", "mta-backup-dev"),
                         // (6) Rename app with namespace prefix and blue suffix to system+user namespace only
                         Arguments.of("prod-web-app-blue", "prod", "mta-backup-prod-web-app", "mta-backup-prod"));
    }

    @ParameterizedTest
    @MethodSource
    void testStep(String applicationName, String namespace, String expectedNewApplicationName, String expectedNamespace) {
        UUID applicationGuid = UUID.randomUUID();
        prepareContext(applicationName, namespace, applicationGuid);

        step.execute(execution);

        assertStepFinishedSuccessfully();
        verify(client).rename(applicationName, expectedNewApplicationName);
        verify(client).updateApplicationMetadata(applicationGuid, Metadata.builder()
                                                                          .label(MtaMetadataLabels.MTA_NAMESPACE,
                                                                                 MtaMetadataUtil.getHashedLabel(expectedNamespace))
                                                                          .annotation(MtaMetadataAnnotations.MTA_NAMESPACE,
                                                                                      expectedNamespace)
                                                                          .build());
    }

    private void prepareContext(String applicationName, String namespace, UUID applicationGuid) {
        CloudApplicationExtended application = ImmutableCloudApplicationExtended.builder()
                                                                                .name(applicationName)
                                                                                .metadata(ImmutableCloudMetadata.builder()
                                                                                                                .guid(applicationGuid)
                                                                                                                .build())
                                                                                .v3Metadata(Metadata.builder()
                                                                                                    .build())
                                                                                .build();
        context.setVariable(Variables.APP_TO_PROCESS, application);
        context.setVariable(Variables.MTA_NAMESPACE, namespace);
    }

    @Override
    protected PrepareApplicationForBackupStep createStep() {
        return new PrepareApplicationForBackupStep();
    }

}
