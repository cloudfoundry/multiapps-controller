package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.core.cf.detect.DeployedMtaDetector;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.ImmutableMtaMetadata;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataLabels;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.persistence.dto.BackupDescriptor;
import org.cloudfoundry.multiapps.controller.persistence.dto.ImmutableBackupDescriptor;
import org.cloudfoundry.multiapps.controller.persistence.query.DescriptorBackupQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.DescriptorBackupService;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.util.ProcessConflictPreventer;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Version;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;

import jakarta.persistence.NoResultException;

class PrepareBackupMtaForDeploymentStepTest extends SyncFlowableStepTest<PrepareBackupMtaForDeploymentStep> {

    private static final String MTA_ID = "test-mta";
    private static final String MTA_VERSION = "1.0.0";

    @Mock
    private DescriptorBackupService descriptorBackupService;
    @Mock
    private DescriptorBackupQuery descriptorBackupQuery;
    @Mock
    private DeployedMtaDetector deployedMtaDetector;
    @Mock
    private OperationService operationService;
    @Mock
    private ProcessConflictPreventer processConflictPreventer;

    private static Stream<Arguments> testStep() {
        return Stream.of(
                         // (1) Backup app exist and persisted descriptor contains same checksum
                         Arguments.of(List.of(new TestApp("app-1", "1"), new TestApp("app-2", "1"), new TestApp("app-3", "1")), "1", false),
                         // (2) Backup app does not have checksum in the metadata
                         Arguments.of(List.of(new TestApp("app-1", null)), "1", true),
                         // (3) Not all backup apps have same checksum
                         Arguments.of(List.of(new TestApp("app-1", "1"), new TestApp("app-2", "2"), new TestApp("app-3", "2")), "1", true),
                         // (4) Missing backup descriptor
                         Arguments.of(List.of(new TestApp("app-1", "2"), new TestApp("app-2", "2")), null, true));
    }

    @ParameterizedTest
    @MethodSource
    void testStep(List<TestApp> testApplications, String backupChecksumInPersistenceLayer, boolean expectedException) {
        DeployedMta backupMta = createBackupMta(testApplications);
        prepareContext(backupMta, backupChecksumInPersistenceLayer);

        if (expectedException) {
            assertThrows(ContentException.class, () -> step.execute(execution));
            return;
        }

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertNotNull(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR));
        assertNotNull(context.getVariable(Variables.MTA_MAJOR_SCHEMA_VERSION));
        assertNotNull(context.getVariable(Variables.DEPLOYED_MTA));
        assertEquals(backupMta, context.getVariable(Variables.BACKUP_MTA));
    }

    private void prepareContext(DeployedMta backupMta, String backupChecksumInPersistenceLayer) {
        context.setVariable(Variables.MTA_ID, MTA_ID);
        when(deployedMtaDetector.detectDeployedMtaByNameAndNamespace(eq(MTA_ID), eq(null),
                                                                     any())).thenReturn(Optional.of(createBackupMta(List.of())));
        when(deployedMtaDetector.detectDeployedMtaByNameAndNamespace(eq(MTA_ID), eq(Constants.MTA_BACKUP_NAMESPACE),
                                                                     any())).thenReturn(Optional.of(backupMta));
        when(descriptorBackupService.createQuery()).thenReturn(descriptorBackupQuery);
        when(descriptorBackupQuery.mtaId(anyString())).thenReturn(descriptorBackupQuery);
        when(descriptorBackupQuery.spaceId(anyString())).thenReturn(descriptorBackupQuery);
        when(descriptorBackupQuery.namespace(any())).thenReturn(descriptorBackupQuery);
        when(descriptorBackupQuery.checksum(anyString())).thenReturn(descriptorBackupQuery);
        if (backupChecksumInPersistenceLayer == null) {
            when(descriptorBackupQuery.singleResult()).thenThrow(NoResultException.class);
            return;
        }
        BackupDescriptor backupDescriptor = ImmutableBackupDescriptor.builder()
                                                                     .descriptor(Mockito.mock(DeploymentDescriptor.class))
                                                                     .mtaId(MTA_ID)
                                                                     .mtaVersion(MTA_VERSION)
                                                                     .spaceId(SPACE_GUID)
                                                                     .checksum(backupChecksumInPersistenceLayer)
                                                                     .build();
        when(descriptorBackupQuery.singleResult()).thenReturn(backupDescriptor);
    }

    private DeployedMta createBackupMta(List<TestApp> testApplications) {
        List<DeployedMtaApplication> deployedApplications = createDeployedMtaApplications(testApplications);
        return ImmutableDeployedMta.builder()
                                   .applications(deployedApplications)
                                   .metadata(ImmutableMtaMetadata.builder()
                                                                 .id(MTA_ID)
                                                                 .version(Version.parseVersion(MTA_VERSION))
                                                                 .build())
                                   .build();
    }

    private List<DeployedMtaApplication> createDeployedMtaApplications(List<TestApp> testApplications) {
        return testApplications.stream()
                               .map(testApp -> ImmutableDeployedMtaApplication.builder()
                                                                              .name(testApp.appName)
                                                                              .moduleName(testApp.appName)
                                                                              .v3Metadata(Metadata.builder()
                                                                                                  .label(MtaMetadataLabels.MTA_DESCRIPTOR_CHECKSUM,
                                                                                                         testApp.checksum)
                                                                                                  .build())
                                                                              .build())
                               .collect(Collectors.toList());
    }

    @Test
    void testThrowingExceptionOnMissingBackupMta() {
        context.setVariable(Variables.MTA_ID, MTA_ID);
        when(deployedMtaDetector.detectDeployedMtaByNameAndNamespace(eq(MTA_ID), eq(null),
                                                                     eq(client))).thenReturn(Optional.of(Mockito.mock(DeployedMta.class)));

        assertThrows(ContentException.class, () -> step.execute(execution));
    }

    @Test
    void testThrowingExceptionOnMissingDeployedMta() {
        context.setVariable(Variables.MTA_ID, MTA_ID);
        when(deployedMtaDetector.detectDeployedMtaByNameAndNamespace(eq(MTA_ID), eq(Constants.MTA_BACKUP_NAMESPACE),
                                                                     eq(client))).thenReturn(Optional.of(Mockito.mock(DeployedMta.class)));

        assertThrows(ContentException.class, () -> step.execute(execution));
    }

    @Override
    protected PrepareBackupMtaForDeploymentStep createStep() {
        PrepareBackupMtaForDeploymentStep step = new PrepareBackupMtaForDeploymentStep(descriptorBackupService,
                                                                                       deployedMtaDetector,
                                                                                       operationService);
        step.conflictPreventerSupplier = service -> processConflictPreventer;
        return step;
    }

    private static class TestApp {
        String appName;
        String checksum;

        TestApp(String appName, String checksum) {
            this.appName = appName;
            this.checksum = checksum;
        }

    }

}
