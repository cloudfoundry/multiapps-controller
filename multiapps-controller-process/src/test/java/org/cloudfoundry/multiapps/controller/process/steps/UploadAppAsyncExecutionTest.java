package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaArchiveElements;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.services.FileContentConsumer;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.process.util.ApplicationArchiveIterator;
import org.cloudfoundry.multiapps.controller.process.util.ApplicationZipBuilder;
import org.cloudfoundry.multiapps.controller.process.util.ArchiveEntryExtractor;
import org.cloudfoundry.multiapps.controller.process.util.ArchiveEntryWithStreamPositions;
import org.cloudfoundry.multiapps.controller.process.util.ImmutableArchiveEntryWithStreamPositions;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.UploadStatusCallback;
import com.sap.cloudfoundry.client.facade.domain.CloudPackage;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudPackage;
import com.sap.cloudfoundry.client.facade.domain.ImmutableDockerData;
import com.sap.cloudfoundry.client.facade.domain.Status;

class UploadAppAsyncExecutionTest extends AsyncStepOperationTest<UploadAppStep> {

    private static final String APP_NAME = "sample-app-backend";
    private static final String APP_FILE = "web.zip";
    private static final ArchiveEntryWithStreamPositions ARCHIVE_ENTRY_WITH_STREAM_POSITIONS = ImmutableArchiveEntryWithStreamPositions.builder()
                                                                                                                                       .name(
                                                                                                                                           APP_FILE)
                                                                                                                                       .startPosition(
                                                                                                                                           37)
                                                                                                                                       .endPosition(
                                                                                                                                           5012)
                                                                                                                                       .compressionMethod(
                                                                                                                                           ArchiveEntryWithStreamPositions.CompressionMethod.DEFLATED)
                                                                                                                                       .isDirectory(
                                                                                                                                           false)
                                                                                                                                       .build();
    private static final String SPACE = "space";
    private static final String APP_ARCHIVE = "sample-app.mtar";
    private static final CloudOperationException CO_EXCEPTION = new CloudOperationException(HttpStatus.BAD_REQUEST);
    private static final UUID APP_GUID = UUID.randomUUID();
    private static final UUID PACKAGE_GUID = UUID.randomUUID();
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
    private final ExecutorService appUploaderThreadPool = mock(ExecutorService.class);

    @TempDir
    Path tempDir;
    private Path appFile;

    private AsyncExecutionState expectedStatus;

    @BeforeEach
    public void setUp() throws Exception {
        prepareFileService();
        prepareContext();
        step.applicationZipBuilder = spy(new ApplicationZipBuilderMock(fileService,
                                                                       new ApplicationArchiveIterator(),
                                                                       new ArchiveEntryExtractor(fileService)));
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
            fileContentConsumer.consume(new FileInputStream(appFile.toFile()));
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
    }

    @Test
    void testFailedUploadWithException() {
        prepareExecutorService();
        context.setVariable(Variables.ARCHIVE_ENTRIES_POSITIONS, List.of(ARCHIVE_ENTRY_WITH_STREAM_POSITIONS));
        when(client.asyncUploadApplicationWithExponentialBackoff(eq(APP_NAME), eq(appFile), any(UploadStatusCallback.class),
                                                                 any())).thenThrow(CO_EXCEPTION);
        expectedStatus = AsyncExecutionState.ERROR;
        Exception exception = assertThrows(SLException.class, this::testExecuteOperations);
        assertEquals("org.cloudfoundry.multiapps.common.SLException: Error while starting async upload of app with name sample-app-backend",
                     exception.getMessage());
        assertFalse(appFile.toFile()
                           .exists());
        assertNull(context.getVariable(Variables.CLOUD_PACKAGE));
    }

    private void prepareExecutorService() {
        when(appUploaderThreadPool.submit(any(Callable.class))).thenAnswer(invocation -> {
            Callable<?> callable = invocation.getArgument(0);
            FutureTask<?> futureTask = new FutureTask<>(callable);
            futureTask.run();
            return futureTask;
        });
    }

    @Test
    void testExtractionOfAppFails() {
        prepareExecutorService();
        context.setVariable(Variables.ARCHIVE_ENTRIES_POSITIONS, Collections.emptyList());
        doThrow(new SLException("Error while reading blob input stream")).when(step.applicationZipBuilder)
                                                                         .extractApplicationInNewArchive(any());
        expectedStatus = AsyncExecutionState.ERROR;
        Exception exception = assertThrows(SLException.class, this::testExecuteOperations);
        assertEquals("org.cloudfoundry.multiapps.common.SLException: Error while reading blob input stream", exception.getMessage());
    }

    @Test
    void testUploadExecutorCapacityIsFull() {
        context.setVariable(Variables.ARCHIVE_ENTRIES_POSITIONS, Collections.emptyList());
        when(appUploaderThreadPool.submit((Callable<Object>) any())).thenThrow(new RejectedExecutionException("Capacity is full"));
        expectedStatus = AsyncExecutionState.RUNNING;
        testExecuteOperations();
    }

    @Test
    void testSuccessfulUpload() {
        prepareExecutorService();
        context.setVariable(Variables.ARCHIVE_ENTRIES_POSITIONS, List.of(ARCHIVE_ENTRY_WITH_STREAM_POSITIONS));
        when(client.asyncUploadApplicationWithExponentialBackoff(eq(APP_NAME), eq(appFile), any(UploadStatusCallback.class),
                                                                 any())).thenReturn(CLOUD_PACKAGE);
        expectedStatus = AsyncExecutionState.FINISHED;
        testExecuteOperations();
        assertEquals(CLOUD_PACKAGE, context.getVariable(Variables.CLOUD_PACKAGE));
        assertTrue(context.getVariable(Variables.APP_CONTENT_CHANGED));
    }

    @Test
    void testSkippingUpload() {
        context.setVariable(Variables.SHOULD_SKIP_APPLICATION_UPLOAD, true);
        expectedStatus = AsyncExecutionState.FINISHED;
        testExecuteOperations();
    }

    @Override
    protected List<AsyncExecution> getAsyncOperations(ProcessContext wrapper) {
        return step.getAsyncStepExecutions(wrapper);
    }

    @Override
    protected void validateOperationExecutionResult(AsyncExecutionState result) {
        assertEquals(expectedStatus.toString(), result.toString());
    }

    @Override
    protected UploadAppStep createStep() {
        return new UploadAppStepMock();
    }

    private class UploadAppStepMock extends UploadAppStep {

        @Override
        protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
            return List.of(new UploadAppAsyncExecution(applicationZipBuilder,
                                                       getProcessLogsPersister(),
                                                       configuration,
                                                       appUploaderThreadPool) {

            });
        }
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
