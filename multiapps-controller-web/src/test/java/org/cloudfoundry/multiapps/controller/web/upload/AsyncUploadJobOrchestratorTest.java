package org.cloudfoundry.multiapps.controller.web.upload;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.api.model.UserCredentials;
import org.cloudfoundry.multiapps.controller.client.util.CheckedSupplier;
import org.cloudfoundry.multiapps.controller.client.util.ResilientOperationExecutor;
import org.cloudfoundry.multiapps.controller.core.helpers.DescriptorParserFacadeFactory;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.AsyncUploadJobEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableAsyncUploadJobEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableFileEntry;
import org.cloudfoundry.multiapps.controller.persistence.query.AsyncUploadJobsQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.AsyncUploadJobService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.web.upload.client.DeployFromUrlRemoteClient;
import org.cloudfoundry.multiapps.controller.web.upload.client.FileFromUrlData;
import org.cloudfoundry.multiapps.controller.web.util.SecurityContextUtil;
import org.cloudfoundry.multiapps.mta.handlers.DescriptorParserFacade;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsyncUploadJobOrchestratorTest {

    private static final String SPACE_GUID = "space-123";
    private static final String NAMESPACE = "test-namespace";
    private static final String FILE_URL = "https://example.com/file.mtar";
    private static final String DECODED_URL = "https://example.com/file.mtar";
    private static final String JOB_ID = "job-123";
    private static final String MTA_ID = "test-mta";
    private static final String FILE_ID = "file-123";
    private static final String USERNAME = "test-user";
    private static final String ERROR_MESSAGE = "Test error message";
    private static final long NORMAL_FILE_SIZE = 1024L;

    @Mock
    private ExecutorService asyncFileUploadExecutor;

    @Mock
    private ExecutorService deployFromUrlExecutor;

    @Mock
    private ApplicationConfiguration applicationConfiguration;

    @Mock
    private AsyncUploadJobService asyncUploadJobService;

    @Mock
    private FileService fileService;

    @Mock
    private DescriptorParserFacadeFactory descriptorParserFactory;

    @Mock
    private DeployFromUrlRemoteClient deployFromUrlRemoteClient;

    @Mock
    private DescriptorParserFacade descriptorParserFacade;

    @Mock
    private DeploymentDescriptor deploymentDescriptor;

    @Mock
    private UserCredentials userCredentials;

    @Mock
    private AsyncUploadJobsQuery asyncUploadJobsQuery;

    private TestableAsyncUploadJobOrchestrator asyncUploadJobExecutor;
    private AtomicBoolean asyncFileExecutorCalled;
    private AtomicBoolean deployFromUrlExecutorCalled;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        asyncFileExecutorCalled = new AtomicBoolean(false);
        deployFromUrlExecutorCalled = new AtomicBoolean(false);

        doAnswer(invocation -> {
            deployFromUrlExecutorCalled.set(true);
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(deployFromUrlExecutor)
          .submit(any(Runnable.class));

        doAnswer(invocation -> {
            asyncFileExecutorCalled.set(true);
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(asyncFileUploadExecutor)
          .submit(any(Runnable.class));

        asyncUploadJobExecutor = new TestableAsyncUploadJobOrchestrator(
            asyncFileUploadExecutor,
            deployFromUrlExecutor,
            applicationConfiguration,
            asyncUploadJobService,
            fileService,
            descriptorParserFactory,
            deployFromUrlRemoteClient
        );

        when(applicationConfiguration.getApplicationInstanceIndex()).thenReturn(0);
        when(applicationConfiguration.getMaxMtaDescriptorSize()).thenReturn(1024L * 1024);

        when(asyncUploadJobService.createQuery()).thenReturn(asyncUploadJobsQuery);
        when(asyncUploadJobsQuery.id(anyString())).thenReturn(asyncUploadJobsQuery);

        when(descriptorParserFactory.getInstance()).thenReturn(descriptorParserFacade);
        when(deploymentDescriptor.getId()).thenReturn(MTA_ID);
        when(userCredentials.getUsername()).thenReturn("testuser");
        when(userCredentials.getPassword()).thenReturn("testpass");
    }

    @Test
    void testSuccessfulJobExecution() throws Exception {
        AsyncUploadJobEntry initialEntry = createInitialJobEntry();
        AsyncUploadJobEntry runningEntry = ImmutableAsyncUploadJobEntry.copyOf(initialEntry)
                                                                       .withState(AsyncUploadJobEntry.State.RUNNING)
                                                                       .withStartedAt(LocalDateTime.now());
        AsyncUploadJobEntry finishedEntry = ImmutableAsyncUploadJobEntry.copyOf(runningEntry)
                                                                        .withState(AsyncUploadJobEntry.State.FINISHED)
                                                                        .withFileId(FILE_ID)
                                                                        .withMtaId(MTA_ID)
                                                                        .withFinishedAt(LocalDateTime.now());

        FileEntry fileEntry = createFileEntry();

        when(asyncUploadJobService.add(any(AsyncUploadJobEntry.class))).thenReturn(initialEntry);
        when(asyncUploadJobService.update(any(AsyncUploadJobEntry.class), any(AsyncUploadJobEntry.class)))
            .thenReturn(runningEntry)
            .thenReturn(finishedEntry);

        when(asyncUploadJobsQuery.singleResult())
            .thenReturn(runningEntry)
            .thenReturn(finishedEntry);

        setupSuccessfulRemoteClientMocks();

        when(fileService.addFile(any(FileEntry.class), any(InputStream.class))).thenReturn(fileEntry);
        when(fileService.processFileContent(anyString(), anyString(), any())).thenReturn(deploymentDescriptor);
        when(descriptorParserFacade.parseDeploymentDescriptor(anyString())).thenReturn(deploymentDescriptor);

        try (MockedStatic<SecurityContextUtil> mockedSecurityContext = mockStatic(SecurityContextUtil.class)) {
            mockedSecurityContext.when(SecurityContextUtil::getUsername)
                                 .thenReturn(USERNAME);

            AsyncUploadJobEntry result = asyncUploadJobExecutor.executeUploadFromUrl(
                SPACE_GUID, NAMESPACE, FILE_URL, DECODED_URL, userCredentials);

            assertEquals(SPACE_GUID, result.getSpaceGuid());
            assertEquals(NAMESPACE, result.getNamespace());
            assertEquals(FILE_URL, result.getUrl());
            assertEquals(AsyncUploadJobEntry.State.INITIAL, result.getState());

            verify(asyncUploadJobService).add(any(AsyncUploadJobEntry.class));
            verify(deployFromUrlExecutor).submit(any(Runnable.class));
            verify(deployFromUrlRemoteClient).downloadFileFromUrl(any());
            verify(fileService).addFile(any(FileEntry.class), any(InputStream.class));
            verify(fileService).processFileContent(anyString(), anyString(), any());
        }
    }

    @Test
    void testJobExecutionWithRemoteClientException() throws Exception {
        AsyncUploadJobEntry initialEntry = createInitialJobEntry();
        AsyncUploadJobEntry runningEntry = ImmutableAsyncUploadJobEntry.copyOf(initialEntry)
                                                                       .withState(AsyncUploadJobEntry.State.RUNNING)
                                                                       .withStartedAt(LocalDateTime.now());
        AsyncUploadJobEntry errorEntry = ImmutableAsyncUploadJobEntry.copyOf(runningEntry)
                                                                     .withState(AsyncUploadJobEntry.State.ERROR);

        when(asyncUploadJobService.add(any(AsyncUploadJobEntry.class))).thenReturn(initialEntry);
        when(asyncUploadJobService.update(any(AsyncUploadJobEntry.class), any(AsyncUploadJobEntry.class)))
            .thenReturn(runningEntry)
            .thenReturn(errorEntry);
        when(asyncUploadJobsQuery.singleResult()).thenReturn(runningEntry);

        when(deployFromUrlRemoteClient.downloadFileFromUrl(any()))
            .thenThrow(new SLException("Remote client error"));

        try (MockedStatic<SecurityContextUtil> mockedSecurityContext = mockStatic(SecurityContextUtil.class)) {
            mockedSecurityContext.when(SecurityContextUtil::getUsername)
                                 .thenReturn(USERNAME);

            AsyncUploadJobEntry result = asyncUploadJobExecutor.executeUploadFromUrl(
                SPACE_GUID, NAMESPACE, FILE_URL, DECODED_URL, userCredentials);

            verify(asyncUploadJobService).add(any(AsyncUploadJobEntry.class));
            verify(deployFromUrlExecutor).submit(any(Runnable.class));
            verify(asyncUploadJobService, times(3)).update(any(AsyncUploadJobEntry.class), any(AsyncUploadJobEntry.class));
        }
    }

    @Test
    void testJobExecutionWithFileServiceException() throws Exception {
        AsyncUploadJobEntry initialEntry = createInitialJobEntry();
        AsyncUploadJobEntry runningEntry = ImmutableAsyncUploadJobEntry.copyOf(initialEntry)
                                                                       .withState(AsyncUploadJobEntry.State.RUNNING)
                                                                       .withStartedAt(LocalDateTime.now());
        AsyncUploadJobEntry errorEntry = ImmutableAsyncUploadJobEntry.copyOf(runningEntry)
                                                                     .withState(AsyncUploadJobEntry.State.ERROR);

        when(asyncUploadJobService.add(any(AsyncUploadJobEntry.class))).thenReturn(initialEntry);
        when(asyncUploadJobService.update(any(AsyncUploadJobEntry.class), any(AsyncUploadJobEntry.class)))
            .thenReturn(runningEntry)
            .thenReturn(errorEntry);
        when(asyncUploadJobsQuery.singleResult()).thenReturn(runningEntry);

        setupSuccessfulRemoteClientMocks();

        when(fileService.addFile(any(FileEntry.class), any(InputStream.class)))
            .thenThrow(new RuntimeException(ERROR_MESSAGE));

        try (MockedStatic<SecurityContextUtil> mockedSecurityContext = mockStatic(SecurityContextUtil.class)) {
            mockedSecurityContext.when(SecurityContextUtil::getUsername)
                                 .thenReturn(USERNAME);

            AsyncUploadJobEntry result = asyncUploadJobExecutor.executeUploadFromUrl(
                SPACE_GUID, NAMESPACE, FILE_URL, DECODED_URL, userCredentials);

            verify(asyncUploadJobService).add(any(AsyncUploadJobEntry.class));
            verify(deployFromUrlExecutor).submit(any(Runnable.class));
            verify(asyncUploadJobService, times(4)).update(any(AsyncUploadJobEntry.class), any(AsyncUploadJobEntry.class));
        }
    }

    @Test
    void testJobExecutionWithDescriptorParsingException() throws Exception {
        AsyncUploadJobEntry initialEntry = createInitialJobEntry();
        AsyncUploadJobEntry runningEntry = ImmutableAsyncUploadJobEntry.copyOf(initialEntry)
                                                                       .withState(AsyncUploadJobEntry.State.RUNNING)
                                                                       .withStartedAt(LocalDateTime.now());
        AsyncUploadJobEntry errorEntry = ImmutableAsyncUploadJobEntry.copyOf(runningEntry)
                                                                     .withState(AsyncUploadJobEntry.State.ERROR);

        FileEntry fileEntry = createFileEntry();

        when(asyncUploadJobService.add(any(AsyncUploadJobEntry.class))).thenReturn(initialEntry);
        when(asyncUploadJobService.update(any(AsyncUploadJobEntry.class), any(AsyncUploadJobEntry.class)))
            .thenReturn(runningEntry)
            .thenReturn(errorEntry);
        when(asyncUploadJobsQuery.singleResult()).thenReturn(runningEntry);

        setupSuccessfulRemoteClientMocks();
        when(fileService.addFile(any(FileEntry.class), any(InputStream.class))).thenReturn(fileEntry);

        when(fileService.processFileContent(anyString(), anyString(), any()))
            .thenThrow(new RuntimeException(ERROR_MESSAGE));

        try (MockedStatic<SecurityContextUtil> mockedSecurityContext = mockStatic(SecurityContextUtil.class)) {
            mockedSecurityContext.when(SecurityContextUtil::getUsername)
                                 .thenReturn(USERNAME);

            AsyncUploadJobEntry result = asyncUploadJobExecutor.executeUploadFromUrl(
                SPACE_GUID, NAMESPACE, FILE_URL, DECODED_URL, userCredentials);

            verify(asyncUploadJobService).add(any(AsyncUploadJobEntry.class));
            verify(deployFromUrlExecutor).submit(any(Runnable.class));
            verify(asyncUploadJobService, times(4)).update(any(AsyncUploadJobEntry.class), any(AsyncUploadJobEntry.class));
        }
    }

    @Test
    void testJobMonitoringWithMultipleUpdates() throws Exception {
        AsyncUploadJobEntry initialEntry = createInitialJobEntry();
        AsyncUploadJobEntry runningEntry1 = ImmutableAsyncUploadJobEntry.copyOf(initialEntry)
                                                                        .withState(AsyncUploadJobEntry.State.RUNNING)
                                                                        .withStartedAt(LocalDateTime.now())
                                                                        .withBytesRead(100L);
        AsyncUploadJobEntry runningEntry2 = ImmutableAsyncUploadJobEntry.copyOf(runningEntry1)
                                                                        .withBytesRead(500L)
                                                                        .withUpdatedAt(LocalDateTime.now());
        AsyncUploadJobEntry runningEntry3 = ImmutableAsyncUploadJobEntry.copyOf(runningEntry2)
                                                                        .withBytesRead(800L)
                                                                        .withUpdatedAt(LocalDateTime.now());
        AsyncUploadJobEntry finishedEntry = ImmutableAsyncUploadJobEntry.copyOf(runningEntry3)
                                                                        .withState(AsyncUploadJobEntry.State.FINISHED)
                                                                        .withFileId(FILE_ID)
                                                                        .withMtaId(MTA_ID)
                                                                        .withFinishedAt(LocalDateTime.now());

        FileEntry fileEntry = createFileEntry();

        when(asyncUploadJobService.add(any(AsyncUploadJobEntry.class))).thenReturn(initialEntry);
        when(asyncUploadJobService.update(any(AsyncUploadJobEntry.class), any(AsyncUploadJobEntry.class)))
            .thenReturn(runningEntry1)
            .thenReturn(runningEntry2)
            .thenReturn(runningEntry3)
            .thenReturn(finishedEntry);

        when(asyncUploadJobsQuery.singleResult())
            .thenReturn(runningEntry1)
            .thenReturn(runningEntry2)
            .thenReturn(runningEntry3)
            .thenReturn(finishedEntry);

        setupSuccessfulRemoteClientMocks();
        when(fileService.addFile(any(FileEntry.class), any(InputStream.class))).thenReturn(fileEntry);
        when(fileService.processFileContent(anyString(), anyString(), any())).thenReturn(deploymentDescriptor);
        when(descriptorParserFacade.parseDeploymentDescriptor(anyString())).thenReturn(deploymentDescriptor);

        try (MockedStatic<SecurityContextUtil> mockedSecurityContext = mockStatic(SecurityContextUtil.class)) {
            mockedSecurityContext.when(SecurityContextUtil::getUsername)
                                 .thenReturn(USERNAME);

            AsyncUploadJobEntry result = asyncUploadJobExecutor.executeUploadFromUrl(
                SPACE_GUID, NAMESPACE, FILE_URL, DECODED_URL, userCredentials);

            verify(asyncUploadJobService, times(4)).update(any(AsyncUploadJobEntry.class), any(AsyncUploadJobEntry.class));
            verify(asyncUploadJobsQuery, times(4)).singleResult();
        }
    }

    @Test
    void testJobCreationWithCorrectProperties() {
        AsyncUploadJobEntry initialEntry = createInitialJobEntry();
        AsyncUploadJobEntry runningEntry = ImmutableAsyncUploadJobEntry.copyOf(initialEntry)
                                                                       .withState(AsyncUploadJobEntry.State.RUNNING)
                                                                       .withStartedAt(LocalDateTime.now());

        when(asyncUploadJobService.add(any(AsyncUploadJobEntry.class))).thenReturn(initialEntry);
        when(asyncUploadJobService.update(any(AsyncUploadJobEntry.class), any(AsyncUploadJobEntry.class)))
            .thenReturn(runningEntry);
        when(asyncUploadJobsQuery.singleResult()).thenReturn(runningEntry);

        // Mock the executor to NOT run the task to avoid the full workflow
        doAnswer(invocation -> {
            deployFromUrlExecutorCalled.set(true);
            // Don't run the runnable - just mark that it was called
            return null;
        }).when(deployFromUrlExecutor)
          .submit(any(Runnable.class));

        try (MockedStatic<SecurityContextUtil> mockedSecurityContext = mockStatic(SecurityContextUtil.class)) {
            mockedSecurityContext.when(SecurityContextUtil::getUsername)
                                 .thenReturn(USERNAME);

            AsyncUploadJobEntry result = asyncUploadJobExecutor.executeUploadFromUrl(
                SPACE_GUID, NAMESPACE, FILE_URL, DECODED_URL, userCredentials);

            assertEquals(SPACE_GUID, result.getSpaceGuid());
            assertEquals(NAMESPACE, result.getNamespace());
            assertEquals(FILE_URL, result.getUrl());
            assertEquals(USERNAME, result.getUser());
            assertEquals(AsyncUploadJobEntry.State.INITIAL, result.getState());
            assertEquals(0, result.getInstanceIndex());
            assertEquals(0L, result.getBytesRead());

            verify(asyncUploadJobService).add(any(AsyncUploadJobEntry.class));
            verify(deployFromUrlExecutor).submit(any(Runnable.class));
        }
    }

    @Test
    void testAsyncExecutorSubmission() {
        AsyncUploadJobEntry initialEntry = createInitialJobEntry();
        when(asyncUploadJobService.add(any(AsyncUploadJobEntry.class))).thenReturn(initialEntry);

        // Mock the executor to NOT run the task to avoid the full workflow
        doAnswer(invocation -> {
            deployFromUrlExecutorCalled.set(true);
            // Don't run the runnable - just mark that it was called
            return null;
        }).when(deployFromUrlExecutor)
          .submit(any(Runnable.class));

        try (MockedStatic<SecurityContextUtil> mockedSecurityContext = mockStatic(SecurityContextUtil.class)) {
            mockedSecurityContext.when(SecurityContextUtil::getUsername)
                                 .thenReturn(USERNAME);

            asyncUploadJobExecutor.executeUploadFromUrl(SPACE_GUID, NAMESPACE, FILE_URL, DECODED_URL, userCredentials);

            verify(deployFromUrlExecutor).submit(any(Runnable.class));
        }
    }

    private AsyncUploadJobEntry createInitialJobEntry() {
        return ImmutableAsyncUploadJobEntry.builder()
                                           .id(JOB_ID)
                                           .user(USERNAME)
                                           .addedAt(LocalDateTime.now())
                                           .spaceGuid(SPACE_GUID)
                                           .namespace(NAMESPACE)
                                           .instanceIndex(0)
                                           .url(FILE_URL)
                                           .state(AsyncUploadJobEntry.State.INITIAL)
                                           .updatedAt(LocalDateTime.now())
                                           .bytesRead(0L)
                                           .build();
    }

    private FileEntry createFileEntry() {
        return ImmutableFileEntry.builder()
                                 .id(FILE_ID)
                                 .space(SPACE_GUID)
                                 .namespace(NAMESPACE)
                                 .name("file.mtar")
                                 .size(BigInteger.valueOf(NORMAL_FILE_SIZE))
                                 .build();
    }

    private void setupSuccessfulRemoteClientMocks() throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream("test content".getBytes());
        FileFromUrlData fileFromUrlData = new FileFromUrlData(inputStream, URI.create(FILE_URL), NORMAL_FILE_SIZE);
        when(deployFromUrlRemoteClient.downloadFileFromUrl(any())).thenReturn(fileFromUrlData);
    }

    private static class TestableAsyncUploadJobOrchestrator extends AsyncUploadJobOrchestrator {

        public TestableAsyncUploadJobOrchestrator(ExecutorService asyncFileUploadExecutor, ExecutorService deployFromUrlExecutor,
                                                  ApplicationConfiguration applicationConfiguration,
                                                  AsyncUploadJobService asyncUploadJobService,
                                                  FileService fileService, DescriptorParserFacadeFactory descriptorParserFactory,
                                                  DeployFromUrlRemoteClient deployFromUrlRemoteClient) {
            super(asyncFileUploadExecutor, deployFromUrlExecutor, applicationConfiguration,
                  asyncUploadJobService, fileService, descriptorParserFactory, deployFromUrlRemoteClient);
        }

        @Override
        protected void waitBetweenUpdates() {
            // do nothing
        }

        @Override
        protected ResilientOperationExecutor getResilientOperationExecutor() {
            return new ResilientOperationExecutor() {
                @Override
                public <T> T execute(CheckedSupplier<T> operation) throws Exception {
                    return operation.get();
                }
            };
        }
    }
}
