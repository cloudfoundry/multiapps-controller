package org.cloudfoundry.multiapps.controller.process.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.ImmutableMtaMetadata;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataLabels;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication.ProductizationState;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.persistence.dto.PreservedDescriptor;
import org.cloudfoundry.multiapps.controller.persistence.query.DescriptorPreserverQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.DescriptorPreserverService;
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

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudApplication;

class ApplicationsPreserveCalculatorTest {

    private static final String MTA_ID = "test-mta";
    private static final Version MTA_VERSION = Version.parseVersion("1.0.0");
    private static final String SPACE_GUID = UUID.randomUUID()
                                                 .toString();

    @Mock
    private DescriptorPreserverService descriptorPreserverService;
    @Mock
    private DescriptorPreserverQuery descriptorPreserverQuery;
    @Mock
    private ProcessContext context;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    private static Stream<Arguments> testCalculateAppsToPreserve() {
        return Stream.of(
                         // (1) Already deployed application match checksum of current deployment descriptor
                         Arguments.of(List.of(new TestApplication("app-1", "app-1-live", "1")), Collections.emptyList(), "1",
                                      List.of("app-1-live"), List.of()),
                         // (2) Current deployment descriptor checksum has different value than deployed mta
                         Arguments.of(List.of(new TestApplication("app-1", "app-1-live", "1"),
                                              new TestApplication("app-1", "app-1-idle", "2", ProductizationState.IDLE)),
                                      Collections.emptyList(), "2", List.of("app-1-live", "app-1-idle"),
                                      List.of(ImmutableCloudApplication.builder()
                                                                       .name("app-1-live")
                                                                       .v3Metadata(Metadata.builder()
                                                                                           .label(MtaMetadataLabels.MTA_DESCRIPTOR_CHECKSUM,
                                                                                                  "1")
                                                                                           .build())
                                                                       .build())),
                         // (3) Current deployment descriptor match checksum of deployed and preserved mta
                         Arguments.of(List.of(new TestApplication("app-1", "app-1-live", "1")),
                                      List.of(new TestApplication("app-1", "mta-preserved-app-1", "1")), "1", List.of("app-1-live"),
                                      Collections.emptyList()),
                         // (4) Current deployment descriptor checksum has different value of deployed and preserved mta
                         Arguments.of(List.of(new TestApplication("app-1", "app-1-live", "2")),
                                      List.of(new TestApplication("app-1",
                                                                  "mta-preserved-app-1",
                                                                  "1")),
                                      "3", List.of("app-1-live", "mta-preserved-app-1"), List.of(ImmutableCloudApplication.builder()
                                                                                                                          .name("app-1-live")
                                                                                                                          .v3Metadata(Metadata.builder()
                                                                                                                                              .label(MtaMetadataLabels.MTA_DESCRIPTOR_CHECKSUM,
                                                                                                                                                     "2")
                                                                                                                                              .build())
                                                                                                                          .build())),
                         // (5) Current deployment descriptor match checksum of deployed mta only
                         Arguments.of(List.of(new TestApplication("app-1", "app-1-live", "2")),
                                      List.of(new TestApplication("app-1", "mta-preserved-app-1", "1")), "2", List.of("app-1-live"),
                                      Collections.emptyList()),
                         // (6) Current deployment descriptor checksum match value of preserved mta
                         Arguments.of(List.of(new TestApplication("app-1", "app-1-live", "2")),
                                      List.of(new TestApplication("app-1", "mta-preserved-app-1", "1")), "1",
                                      List.of("app-1-live", "app-1-idle"), Collections.emptyList()));
    }

    @ParameterizedTest
    @MethodSource
    void testCalculateAppsToPreserve(List<TestApplication> deployedApplications, List<TestApplication> preservedApplications,
                                     String currentDeploymentDescriptorChecksum, List<String> appNamesToUndeploy,
                                     List<CloudApplication> expectedAppsToPreserve) {
        DeployedMta deployedMta = getDeployedMta(deployedApplications);
        DeployedMta preservedMta = getDeployedMta(preservedApplications);

        ApplicationsPreserveCalculator calculator = new ApplicationsPreserveCalculator(deployedMta,
                                                                                       preservedMta,
                                                                                       descriptorPreserverService);

        List<CloudApplication> appsToUndeploy = getAppsToUndeploy(deployedMta.getApplications(), appNamesToUndeploy);
        List<CloudApplication> appsToPreserve = calculator.calculateAppsToPreserve(appsToUndeploy, currentDeploymentDescriptorChecksum);

        assertEquals(expectedAppsToPreserve, appsToPreserve);
    }

    private static Stream<Arguments> testCalculateAppsToUndeploy() {
        return Stream.of(
                         // (1) Preserved apps exist and new version will be preserved and older one needs to be deleted
                         Arguments.of(List.of(new TestApplication("app-1", "mta-preserved-app-1", "1")), List.of("app-1-idle"), true,
                                      List.of(ImmutableCloudApplication.builder()
                                                                       .name("mta-preserved-app-1")
                                                                       .v3Metadata(Metadata.builder()
                                                                                           .label(MtaMetadataLabels.MTA_DESCRIPTOR_CHECKSUM,
                                                                                                  "1")
                                                                                           .build())
                                                                       .build())),
                         // (2) Preserved apps does not exist and there is no need to delete applications
                         Arguments.of(Collections.emptyList(), List.of("special-app"), false, Collections.emptyList()),
                         // (3) There is no specified apps to preserve and deletion will be skipped for already existing preserved apps
                         Arguments.of(List.of(new TestApplication("app-1", "mta-preserved-app-1", "1"),
                                              new TestApplication("app-2", "mta-preserved-app-2", "1")),
                                      Collections.emptyList(), true, Collections.emptyList()),
                         // (4) Preserved apps exist with same name and won't be deleted
                         Arguments.of(List.of(new TestApplication("app-1", "mta-preserved-app-1", "1"),
                                              new TestApplication("app-2", "mta-preserved-app-2", "1")),
                                      List.of("mta-preserved-app-1", "mta-preserved-app-2"), true, Collections.emptyList()),
                         // (5) Preserved apps exist but descriptor is not available in db
                         Arguments.of(List.of(new TestApplication("app-1", "mta-preserved-app-1", "2"),
                                              new TestApplication("app-2", "mta-preserved-app-2", "2")),
                                      Collections.emptyList(), false, List.of(ImmutableCloudApplication.builder()
                                                                                                       .name("mta-preserved-app-1")
                                                                                                       .v3Metadata(Metadata.builder()
                                                                                                                           .label(MtaMetadataLabels.MTA_DESCRIPTOR_CHECKSUM,
                                                                                                                                  "2")
                                                                                                                           .build())
                                                                                                       .build(),
                                                                              ImmutableCloudApplication.builder()
                                                                                                       .name("mta-preserved-app-2")
                                                                                                       .v3Metadata(Metadata.builder()
                                                                                                                           .label(MtaMetadataLabels.MTA_DESCRIPTOR_CHECKSUM,
                                                                                                                                  "2")
                                                                                                                           .build())
                                                                                                       .build())));
    }

    @ParameterizedTest
    @MethodSource
    void testCalculateAppsToUndeploy(List<TestApplication> deployedPreservedApps, List<String> appNameToPreserve,
                                     boolean isDescriptorAvailableInDb, List<CloudApplication> expectedAppsToUndeploy) {
        DeployedMta preservedMta = getDeployedMta(deployedPreservedApps);

        when(context.getVariable(Variables.MTA_ID)).thenReturn(MTA_ID);
        when(context.getVariable(Variables.SPACE_GUID)).thenReturn(SPACE_GUID);
        when(descriptorPreserverService.createQuery()).thenReturn(descriptorPreserverQuery);
        when(descriptorPreserverQuery.mtaId(anyString())).thenReturn(descriptorPreserverQuery);
        when(descriptorPreserverQuery.spaceId(anyString())).thenReturn(descriptorPreserverQuery);
        when(descriptorPreserverQuery.namespace(any())).thenReturn(descriptorPreserverQuery);
        when(descriptorPreserverQuery.checksum(anyString())).thenReturn(descriptorPreserverQuery);
        when(descriptorPreserverQuery.list()).thenReturn(isDescriptorAvailableInDb ? List.of(Mockito.mock(PreservedDescriptor.class))
            : Collections.emptyList());

        ApplicationsPreserveCalculator calculator = new ApplicationsPreserveCalculator(null, preservedMta, descriptorPreserverService);

        List<CloudApplication> appsToPreserve = appNameToPreserve.stream()
                                                                 .map(appName -> ImmutableCloudApplication.builder()
                                                                                                          .name(appName)
                                                                                                          .build())
                                                                 .collect(Collectors.toList());
        List<CloudApplication> appsToUndeploy = calculator.calculateAppsToUndeploy(context, appsToPreserve)
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
                                                                       .v3Metadata(Metadata.builder()
                                                                                           .label(MtaMetadataLabels.MTA_DESCRIPTOR_CHECKSUM,
                                                                                                  application.metadataDescriptorChecksum)
                                                                                           .build())
                                                                       .productizationState(application.productizationState)
                                                                       .build());
        }
        return ImmutableDeployedMta.builder()
                                   .applications(deployedMtaApplications)
                                   .metadata(ImmutableMtaMetadata.builder()
                                                                 .id(MTA_ID)
                                                                 .version(MTA_VERSION)
                                                                 .build())

                                   .build();
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
        String metadataDescriptorChecksum;
        ProductizationState productizationState;

        TestApplication(String moduleName, String appName, String metadataDescriptorChecksum) {
            this.moduleName = moduleName;
            this.appName = appName;
            this.metadataDescriptorChecksum = metadataDescriptorChecksum;
            this.productizationState = ProductizationState.LIVE;
        }

        TestApplication(String moduleName, String appName, String metadataDescriptorChecksum, ProductizationState productizationState) {
            this.moduleName = moduleName;
            this.appName = appName;
            this.metadataDescriptorChecksum = metadataDescriptorChecksum;
            this.productizationState = productizationState;
        }

    }

}
