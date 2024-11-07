package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.Constants;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaArchiveElements;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.services.FileContentConsumer;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.process.util.ApplicationArchiveIterator;
import org.cloudfoundry.multiapps.controller.process.util.ApplicationDigestCalculator;
import org.cloudfoundry.multiapps.controller.process.util.ApplicationZipBuilder;
import org.cloudfoundry.multiapps.controller.process.util.ArchiveEntryExtractor;
import org.cloudfoundry.multiapps.controller.process.util.ArchiveEntryWithStreamPositions;
import org.cloudfoundry.multiapps.controller.process.util.CloudPackagesGetter;
import org.cloudfoundry.multiapps.controller.process.util.ImmutableArchiveEntryWithStreamPositions;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloudfoundry.client.facade.UploadStatusCallback;
import com.sap.cloudfoundry.client.facade.domain.CloudBuild;
import com.sap.cloudfoundry.client.facade.domain.CloudPackage;
import com.sap.cloudfoundry.client.facade.domain.DropletInfo;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudBuild;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudPackage;
import com.sap.cloudfoundry.client.facade.domain.ImmutableDockerData;
import com.sap.cloudfoundry.client.facade.domain.ImmutableDropletInfo;
import com.sap.cloudfoundry.client.facade.domain.Status;

class UploadAppStepGeneralTest extends SyncFlowableStepTest<UploadAppStep> {

    private static final String APP_NAME = "sample-app-backend";
    private static final String APP_FILE = "web.zip";
    private static final ArchiveEntryWithStreamPositions ARCHIVE_ENTRY_WITH_STREAM_POSITIONS = ImmutableArchiveEntryWithStreamPositions.builder()
                                                                                                                                       .name(APP_FILE)
                                                                                                                                       .startPosition(37)
                                                                                                                                       .endPosition(5012)
                                                                                                                                       .compressionMethod(ArchiveEntryWithStreamPositions.CompressionMethod.DEFLATED)
                                                                                                                                       .isDirectory(false)
                                                                                                                                       .build();
    private static final String SPACE = "space";
    private static final String APP_ARCHIVE = "sample-app.mtar";
    private static final String CURRENT_MODULE_DIGEST = "439B99DFFD0583200D5D21F4CD1BF035";
    private static final String NEW_MODULE_DIGEST = "539B99DFFD0583200D5D21F4CD1BF035";
    private static final UUID APP_GUID = UUID.randomUUID();
    private static final UUID PACKAGE_GUID = UUID.randomUUID();
    private static final UUID DROPLET_GUID = UUID.randomUUID();
    private static final CloudPackage CLOUD_PACKAGE = ImmutableCloudPackage.builder()
                                                                           .metadata(ImmutableCloudMetadata.builder()
                                                                                                           .createdAt(LocalDateTime.now())
                                                                                                           .guid(PACKAGE_GUID)
                                                                                                           .build())
                                                                           .type(CloudPackage.Type.DOCKER)
                                                                           .data(ImmutableDockerData.builder()
                                                                                                    .image("cloudfoundry/test")
                                                                                                    .build())
                                                                           .status(Status.AWAITING_UPLOAD)
                                                                           .build();
    private final MtaArchiveElements mtaArchiveElements = new MtaArchiveElements();
    private final CloudPackagesGetter cloudPackagesGetter = mock(CloudPackagesGetter.class);
    @TempDir
    Path tempDir;
    private Path appFile;

    private static Stream<Arguments> testWithAvailableExpiredCloudPackageAndDifferentContent() {
        return Stream.of(Arguments.of(CURRENT_MODULE_DIGEST), Arguments.of(NEW_MODULE_DIGEST));
    }

    private static Stream<Arguments> testWithBuildStates() {
        return Stream.of(Arguments.of(List.of(ImmutableCloudBuild.builder()
                                                                 .metadata(ImmutableCloudMetadata.builder()
                                                                                                 .createdAt(LocalDateTime.now())
                                                                                                 .build())
                                                                 .state(CloudBuild.State.STAGED)
                                                                 .dropletInfo(ImmutableDropletInfo.builder()
                                                                                                  .guid(UUID.randomUUID())
                                                                                                  .build())
                                                                 .build()),
                                      StepPhase.DONE, null),
                         Arguments.of(List.of(ImmutableCloudBuild.builder()
                                                                 .metadata(ImmutableCloudMetadata.builder()
                                                                                                 .createdAt(LocalDateTime.now())
                                                                                                 .build())
                                                                 .state(CloudBuild.State.FAILED)
                                                                 .dropletInfo(ImmutableDropletInfo.builder()
                                                                                                  .guid(UUID.randomUUID())
                                                                                                  .build())
                                                                 .build()),
                                      StepPhase.POLL, CLOUD_PACKAGE),
                         Arguments.of(List.of(ImmutableCloudBuild.builder()
                                                                 .state(CloudBuild.State.STAGING)
                                                                 .dropletInfo(ImmutableDropletInfo.builder()
                                                                                                  .guid(UUID.randomUUID())
                                                                                                  .build())
                                                                 .build()),
                                      StepPhase.POLL, CLOUD_PACKAGE));
    }

    @BeforeEach
    public void setUp() throws Exception {
        prepareFileService();
        prepareContext();
        step.applicationZipBuilder = spy(new ApplicationZipBuilderMock(fileService,
                                                                       new ApplicationArchiveIterator(),
                                                                       new ArchiveEntryExtractor(fileService)));
        step.applicationDigestCalculator = mock(ApplicationDigestCalculator.class);
    }

    @SuppressWarnings("rawtypes")
    private void prepareFileService() throws Exception {
        appFile = Paths.get(tempDir.toString() + File.separator + APP_FILE);
        if (!appFile.toFile()
                    .exists()) {
            Files.createFile(appFile);
        }
        doAnswer(invocation -> {
            FileContentConsumer fileContentConsumer = invocation.getArgument(1);
            fileContentConsumer.consume(null);
            return null;
        }).when(fileService)
          .consumeFileContentWithOffset(any(), any());
    }

    private void prepareContext() {
        CloudApplicationExtended app = ImmutableCloudApplicationExtended.builder()
                                                                        .metadata(ImmutableCloudMetadata.builder()
                                                                                                        .guid(APP_GUID)
                                                                                                        .build())
                                                                        .name(APP_NAME)
                                                                        .moduleName(APP_NAME)
                                                                        .build();
        context.setVariable(Variables.APP_TO_PROCESS, app);
        context.setVariable(Variables.MODULES_INDEX, 0);
        context.setVariable(Variables.APP_ARCHIVE_ID, APP_ARCHIVE);
        context.setVariable(Variables.SPACE_GUID, SPACE);
        mtaArchiveElements.addModuleFileName(APP_NAME, APP_FILE);
        context.setVariable(Variables.MTA_ARCHIVE_ELEMENTS, mtaArchiveElements);
        context.setVariable(Variables.VCAP_APP_PROPERTIES_CHANGED, false);
        when(configuration.getMaxResourceFileSize()).thenReturn(ApplicationConfiguration.DEFAULT_MAX_RESOURCE_FILE_SIZE);
        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, descriptor);
        context.setVariable(Variables.ARCHIVE_ENTRIES_POSITIONS, List.of(ARCHIVE_ENTRY_WITH_STREAM_POSITIONS));
    }

    @AfterEach
    public void tearDown() {
        FileUtils.deleteQuietly(appFile.getParent()
                                       .toFile());
    }

    @Test
    void testSuccessfulUpload() {
        prepareClients(NEW_MODULE_DIGEST);
        step.execute(execution);
        assertEquals(StepPhase.POLL.toString(), getExecutionStatus());
    }

    @Test
    void testWithAvailableValidCloudPackage() {
        prepareClients(CURRENT_MODULE_DIGEST);
        mockCloudPackagesGetter(createCloudPackage(Status.PROCESSING_UPLOAD));
        step.execute(execution);
        assertEquals(StepPhase.POLL.toString(), getExecutionStatus());
    }

    @Test
    void testWithAvailableFailedLatestPackageAndNonChangedApplicationContent() {
        prepareClients(CURRENT_MODULE_DIGEST);
        mockCloudPackagesGetter(createCloudPackage(Status.FAILED));
        step.execute(execution);
        assertEquals(StepPhase.POLL.toString(), getExecutionStatus());
    }

    @Test
    void testSkippingDigestCalculation() {
        prepareClients(CURRENT_MODULE_DIGEST);
        context.setVariable(Variables.SKIP_APP_DIGEST_CALCULATION, true);
        Map<String, String> deployAttributes = Map.of(Constants.ATTR_APP_CONTENT_DIGEST, CURRENT_MODULE_DIGEST);
        when(client.getApplicationEnvironment(APP_GUID)).thenReturn(Map.of(Constants.ENV_DEPLOY_ATTRIBUTES,
                                                                           JsonUtil.toJson(deployAttributes)));
        step.execute(execution);
        assertEquals(StepPhase.POLL.toString(), getExecutionStatus());
        assertTrue(context.getVariable(Variables.SHOULD_UPDATE_APPLICATION_DIGEST));
    }

    @MethodSource
    @ParameterizedTest
    void testWithAvailableExpiredCloudPackageAndDifferentContent(String moduleDigest) {
        prepareClients(moduleDigest);
        mockCloudPackagesGetter(createCloudPackage(Status.EXPIRED));
        step.execute(execution);
        assertEquals(StepPhase.POLL.toString(), getExecutionStatus());
    }

    @MethodSource
    @ParameterizedTest
    void testWithBuildStates(List<CloudBuild> builds, StepPhase stepPhase, CloudPackage cloudPackage) {
        if (cloudPackage == null) {
            when(client.getApplicationEnvironment(any(UUID.class))).thenReturn(Map.of("DEPLOY_ATTRIBUTES", "{\"app-content-digest\":\""
                + CURRENT_MODULE_DIGEST + "\"}"));
            var dropletPackage = createCloudPackage(Status.READY);
            mockCloudPackagesGetter(dropletPackage);
            when(cloudPackagesGetter.getMostRecentAppPackage(any(), any())).thenReturn(Optional.of(dropletPackage));
        }
        when(client.getCurrentDropletForApplication(APP_GUID)).thenReturn(createDropletInfo(DROPLET_GUID, PACKAGE_GUID));
        when(client.getBuildsForApplication(any())).thenReturn(builds);
        prepareClients(CURRENT_MODULE_DIGEST);
        step.execute(execution);
        assertEquals(stepPhase.toString(), getExecutionStatus());
    }

    private CloudPackage createCloudPackage(Status status) {
        ImmutableCloudMetadata cloudMetadata = ImmutableCloudMetadata.builder()
                                                                     .guid(PACKAGE_GUID)
                                                                     .build();
        return ImmutableCloudPackage.builder()
                                    .metadata(cloudMetadata)
                                    .status(status)
                                    .build();
    }

    private DropletInfo createDropletInfo(UUID guid, UUID packageGuid) {
        return ImmutableDropletInfo.builder()
                                   .guid(guid)
                                   .packageGuid(packageGuid)
                                   .build();
    }

    private void mockCloudPackagesGetter(CloudPackage cloudPackage) {
        when(cloudPackagesGetter.getAppPackage(any(), any())).thenReturn(Optional.of(cloudPackage));
        when(cloudPackagesGetter.getMostRecentAppPackage(any(), any())).thenReturn(Optional.of(cloudPackage));
    }

    private void prepareClients(String applicationDigest) {
        when(client.asyncUploadApplicationWithExponentialBackoff(eq(APP_NAME), eq(appFile), any(UploadStatusCallback.class),
                                                                 any())).thenReturn(CLOUD_PACKAGE);
        CloudApplicationExtended application = createApplication(applicationDigest);
        when(client.getApplicationEnvironment(APP_GUID)).thenReturn(application.getEnv());
        when(client.getApplication(APP_NAME)).thenReturn(application);
        when(step.applicationDigestCalculator.calculateApplicationDigest(any())).thenReturn(applicationDigest);
    }

    private CloudApplicationExtended createApplication(String digest) {
        Map<String, Object> deployAttributes = new HashMap<>();
        deployAttributes.put(Constants.ATTR_APP_CONTENT_DIGEST, digest);
        return ImmutableCloudApplicationExtended.builder()
                                                .metadata(ImmutableCloudMetadata.builder()
                                                                                .guid(APP_GUID)
                                                                                .build())
                                                .name(UploadAppStepGeneralTest.APP_NAME)
                                                .moduleName(UploadAppStepGeneralTest.APP_NAME)
                                                .putEnv(Constants.ENV_DEPLOY_ATTRIBUTES, JsonUtil.toJson(deployAttributes))
                                                .build();
    }

    @Override
    protected UploadAppStep createStep() {
        return new UploadAppStep();
    }

    private class ApplicationZipBuilderMock extends ApplicationZipBuilder {

        public ApplicationZipBuilderMock(FileService fileService, ApplicationArchiveIterator applicationArchiveIterator,
                                         ArchiveEntryExtractor archiveEntryExtractor) {
            super(fileService, applicationArchiveIterator, archiveEntryExtractor);
        }

        @Override
        protected Path createTempFile() {
            return appFile;
        }
    }

}
