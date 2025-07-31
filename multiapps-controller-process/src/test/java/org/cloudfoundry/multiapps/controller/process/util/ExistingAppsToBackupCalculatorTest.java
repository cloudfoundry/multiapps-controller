package org.cloudfoundry.multiapps.controller.process.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudApplication;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.ImmutableMtaMetadata;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataAnnotations;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication.ProductizationState;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.persistence.dto.BackupDescriptor;
import org.cloudfoundry.multiapps.controller.persistence.query.DescriptorBackupQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.DescriptorBackupService;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class ExistingAppsToBackupCalculatorTest {

    private static final String MTA_ID = "test-mta";
    private static final String SPACE_GUID = UUID.randomUUID()
                                                 .toString();

    @Mock
    private DescriptorBackupService descriptorBackupService;
    @Mock
    private DescriptorBackupQuery descriptorBackupQuery;
    @Mock
    private ProcessContext context;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    private static Stream<Arguments> testCalculateExistingAppsToBackup() {
        return Stream.of(
            // (1) Already deployed application match version of current deployment descriptor
            Arguments.of(List.of(new TestApplication("app-1", "app-1-live", "1")), Collections.emptyList(), "1", true,
                         List.of("app-1-live"), List.of()),
            // (2) Current deployment descriptor version has different value than deployed mta
            Arguments.of(List.of(new TestApplication("app-1", "app-1-live", "1"),
                                 new TestApplication("app-1", "app-1-idle", "2", ProductizationState.IDLE)),
                         Collections.emptyList(), "2", true, List.of("app-1-live", "app-1-idle"),
                         List.of(ImmutableCloudApplication.builder()
                                                          .name("app-1-live")
                                                          .v3Metadata(Metadata.builder()
                                                                              .annotation(MtaMetadataAnnotations.MTA_VERSION,
                                                                                          "1")
                                                                              .build())
                                                          .build())),
            // (3) Current deployment descriptor match version of deployed and backup mta
            Arguments.of(List.of(new TestApplication("app-1", "app-1-live", "1")),
                         List.of(new TestApplication("app-1", "mta-backup-app-1", "1")), "1", true, List.of("app-1-live"),
                         Collections.emptyList()),
            // (4) Current deployment descriptor version has different value of deployed and backup mta
            Arguments.of(List.of(new TestApplication("app-1", "app-1-live", "2")),
                         List.of(new TestApplication("app-1",
                                                     "mta-backup-app-1",
                                                     "1")),
                         "3", true, List.of("app-1-live", "mta-backup-app-1"), List.of(ImmutableCloudApplication.builder()
                                                                                                                .name("app-1-live")
                                                                                                                .v3Metadata(
                                                                                                                    Metadata.builder()
                                                                                                                            .annotation(
                                                                                                                                MtaMetadataAnnotations.MTA_VERSION,
                                                                                                                                "2")
                                                                                                                            .build())
                                                                                                                .build())),
            // (5) Current deployment descriptor match version of deployed mta only
            Arguments.of(List.of(new TestApplication("app-1", "app-1-live", "2")),
                         List.of(new TestApplication("app-1", "mta-backup-app-1", "1")), "2", true, List.of("app-1-live"),
                         Collections.emptyList()),
            // (6) Current deployment descriptor version match value of backup mta
            Arguments.of(List.of(new TestApplication("app-1", "app-1-live", "2")),
                         List.of(new TestApplication("app-1", "mta-backup-app-1", "1")), "1", true,
                         List.of("app-1-live", "app-1-idle"), Collections.emptyList()),
            // (7) Deployed mta does not have backup descriptor in db and won't be preserved
            Arguments.of(List.of(new TestApplication("app-1", "app-1", "1"), new TestApplication("app-2", "app-2", "1")),
                         Collections.emptyList(), "2", false, Collections.emptyList(), Collections.emptyList()),
            // (8) Deployed mta contains applications with different versions and existing backup
            Arguments.of(List.of(new TestApplication("app-1", "app-1-live", "2"),
                                 new TestApplication("app-1", "app-1-idle", "3", ProductizationState.IDLE)),
                         List.of(new TestApplication("app-1", "mta-backup-app-1", "1")), "3", true, List.of("app-1-live"),
                         List.of(ImmutableCloudApplication.builder()
                                                          .name("app-1-live")
                                                          .v3Metadata(Metadata.builder()
                                                                              .annotation(MtaMetadataAnnotations.MTA_VERSION,
                                                                                          "2")
                                                                              .build())
                                                          .build())),
            // (9) Missing deployed mta and existing backup
            Arguments.of(Collections.emptyList(), List.of(new TestApplication("app-1", "mta-backup-app-1", "1")), "2", false,
                         Collections.emptyList(), Collections.emptyList()),
            // (10) Deployed mta contains applications with missing versions and existing backup
            Arguments.of(List.of(new TestApplication("app-1", "app-1", null)),
                         List.of(new TestApplication("app-1", "mta-backup-app-1", "1")), "2", false, Collections.emptyList(),
                         Collections.emptyList()));
    }

    @ParameterizedTest
    @MethodSource
    void testCalculateExistingAppsToBackup(List<TestApplication> deployedApplications, List<TestApplication> backupApplications,
                                           String mtaVersionOfCurrentDescriptor, boolean isDescriptorAvailableInDb,
                                           List<String> appNamesToUndeploy, List<CloudApplication> expectedAppsToBackup) {
        DeployedMta deployedMta = getDeployedMta(deployedApplications);
        DeployedMta backupMta = getDeployedMta(backupApplications);

        prepareContext(isDescriptorAvailableInDb);

        ExistingAppsToBackupCalculator calculator = new ExistingAppsToBackupCalculator(deployedMta, backupMta, descriptorBackupService);

        List<CloudApplication> appsToUndeploy = getAppsToUndeploy(deployedMta == null ? Collections.emptyList()
                                                                      : deployedMta.getApplications(), appNamesToUndeploy);
        List<CloudApplication> appsToBackup = calculator.calculateExistingAppsToBackup(context, appsToUndeploy,
                                                                                       mtaVersionOfCurrentDescriptor);

        assertEquals(expectedAppsToBackup, appsToBackup);
    }

    private static Stream<Arguments> testCalculateAppsToUndeploy() {
        return Stream.of(
            // (1) Backup apps exist and new version will be backup and older one needs to be deleted
            Arguments.of(List.of(new TestApplication("app-1", "mta-backup-app-1", "1")), List.of("app-1-idle"), true,
                         List.of(ImmutableCloudApplication.builder()
                                                          .name("mta-backup-app-1")
                                                          .v3Metadata(Metadata.builder()
                                                                              .annotation(MtaMetadataAnnotations.MTA_VERSION,
                                                                                          "1")
                                                                              .build())
                                                          .build())),
            // (2) Backup apps does not exist and there is no need to delete applications
            Arguments.of(Collections.emptyList(), List.of("special-app"), false, Collections.emptyList()),
            // (3) There is no specified apps to backup and deletion will be skipped for already existing backup apps
            Arguments.of(List.of(new TestApplication("app-1", "mta-backup-app-1", "1"),
                                 new TestApplication("app-2", "mta-backup-app-2", "1")),
                         Collections.emptyList(), true, Collections.emptyList()),
            // (4) Backup apps exist with same name and won't be deleted
            Arguments.of(List.of(new TestApplication("app-1", "mta-backup-app-1", "1"),
                                 new TestApplication("app-2", "mta-backup-app-2", "1")),
                         List.of("mta-backup-app-1", "mta-backup-app-2"), true, Collections.emptyList()),
            // (5) Backup apps exist but descriptor is not available in db
            Arguments.of(List.of(new TestApplication("app-1", "mta-backup-app-1", "2"),
                                 new TestApplication("app-2", "mta-backup-app-2", "2")),
                         Collections.emptyList(), false, List.of(ImmutableCloudApplication.builder()
                                                                                          .name("mta-backup-app-1")
                                                                                          .v3Metadata(Metadata.builder()
                                                                                                              .annotation(
                                                                                                                  MtaMetadataAnnotations.MTA_VERSION,
                                                                                                                  "2")
                                                                                                              .build())
                                                                                          .build(),
                                                                 ImmutableCloudApplication.builder()
                                                                                          .name("mta-backup-app-2")
                                                                                          .v3Metadata(Metadata.builder()
                                                                                                              .annotation(
                                                                                                                  MtaMetadataAnnotations.MTA_VERSION,
                                                                                                                  "2")
                                                                                                              .build())
                                                                                          .build())));
    }

    @ParameterizedTest
    @MethodSource
    void testCalculateAppsToUndeploy(List<TestApplication> deployedBackupApps, List<String> appsNameToBackup,
                                     boolean isDescriptorAvailableInDb, List<CloudApplication> expectedAppsToUndeploy) {
        DeployedMta backupMta = getDeployedMta(deployedBackupApps);

        prepareContext(isDescriptorAvailableInDb);

        ExistingAppsToBackupCalculator calculator = new ExistingAppsToBackupCalculator(null, backupMta, descriptorBackupService);

        List<CloudApplication> appsToBackup = appsNameToBackup.stream()
                                                              .map(appName -> ImmutableCloudApplication.builder()
                                                                                                       .name(appName)
                                                                                                       .build())
                                                              .collect(Collectors.toList());
        List<CloudApplication> appsToUndeploy = calculator.calculateAppsToUndeploy(context, appsToBackup)
                                                          .stream()
                                                          .map(appToUndeploy -> ImmutableCloudApplication.copyOf(appToUndeploy))
                                                          .collect(Collectors.toList());
        assertEquals(expectedAppsToUndeploy, appsToUndeploy);
    }

    private DeployedMta getDeployedMta(List<TestApplication> deployedApplications) {
        if (deployedApplications.isEmpty()) {
            return null;
        }
        List<DeployedMtaApplication> deployedMtaApplications = new ArrayList<>();
        for (TestApplication application : deployedApplications) {
            deployedMtaApplications.add(ImmutableDeployedMtaApplication.builder()
                                                                       .moduleName(application.moduleName)
                                                                       .name(application.appName)
                                                                       .v3Metadata(application.mtaVersion != null ? Metadata.builder()
                                                                                                                            .annotation(
                                                                                                                                MtaMetadataAnnotations.MTA_VERSION,
                                                                                                                                application.mtaVersion)
                                                                                                                            .build()
                                                                                       : Metadata.builder()
                                                                                                 .annotations(Collections.emptyMap())
                                                                                                 .build())
                                                                       .productizationState(application.productizationState)
                                                                       .build());
        }
        String mtaVersion = deployedApplications.get(0).mtaVersion;
        return ImmutableDeployedMta.builder()
                                   .applications(deployedMtaApplications)
                                   .metadata(ImmutableMtaMetadata.builder()
                                                                 .id(MTA_ID)
                                                                 .version(mtaVersion != null && deployedApplications.stream()
                                                                                                                    .allMatch(
                                                                                                                        deployedApplication -> mtaVersion.equals(
                                                                                                                            deployedApplication.mtaVersion))
                                                                              ? Version.parseVersion(mtaVersion)
                                                                              : null)
                                                                 .build())

                                   .build();
    }

    private void prepareContext(boolean isDescriptorAvailableInDb) {
        when(context.getVariable(Variables.MTA_ID)).thenReturn(MTA_ID);
        when(context.getVariable(Variables.SPACE_GUID)).thenReturn(SPACE_GUID);
        when(descriptorBackupService.createQuery()).thenReturn(descriptorBackupQuery);
        when(descriptorBackupQuery.mtaId(anyString())).thenReturn(descriptorBackupQuery);
        when(descriptorBackupQuery.spaceId(anyString())).thenReturn(descriptorBackupQuery);
        when(descriptorBackupQuery.namespace(any())).thenReturn(descriptorBackupQuery);
        when(descriptorBackupQuery.mtaVersion(anyString())).thenReturn(descriptorBackupQuery);
        when(descriptorBackupQuery.list()).thenReturn(isDescriptorAvailableInDb ? List.of(Mockito.mock(BackupDescriptor.class))
                                                          : Collections.emptyList());
    }

    private List<CloudApplication> getAppsToUndeploy(List<DeployedMtaApplication> deployedApplications, List<String> appNamesToUndeploy) {
        return deployedApplications.stream()
                                   .filter(deployedApplication -> appNamesToUndeploy.contains(deployedApplication.getName()))
                                   .map(ImmutableCloudApplication::copyOf)
                                   .collect(Collectors.toList());
    }

    private static class TestApplication {
        String moduleName;
        String appName;
        String mtaVersion;
        ProductizationState productizationState;

        TestApplication(String moduleName, String appName, String mtaVersion) {
            this.moduleName = moduleName;
            this.appName = appName;
            this.mtaVersion = mtaVersion;
            this.productizationState = ProductizationState.LIVE;
        }

        TestApplication(String moduleName, String appName, String mtaVersion, ProductizationState productizationState) {
            this.moduleName = moduleName;
            this.appName = appName;
            this.mtaVersion = mtaVersion;
            this.productizationState = productizationState;
        }

    }

}
