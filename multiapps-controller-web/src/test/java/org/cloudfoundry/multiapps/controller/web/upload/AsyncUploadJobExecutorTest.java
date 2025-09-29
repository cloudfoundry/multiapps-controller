package org.cloudfoundry.multiapps.controller.web.upload;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.OptionalLong;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.cloudfoundry.multiapps.controller.api.model.UserCredentials;
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
import org.cloudfoundry.multiapps.controller.web.Constants;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.cloudfoundry.multiapps.controller.web.util.SecurityContextUtil;
import org.cloudfoundry.multiapps.mta.handlers.DescriptorParserFacade;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsyncUploadJobExecutorTest {

    private static final String SPACE_GUID = "space-123";
    private static final String NAMESPACE = "test-namespace";
    private static final String FILE_URL = "https://example.com/file.mtar";
    private static final String INSECURE_URL = "http://example.com/file.mtar";
    private static final String DECODED_URL = "https://example.com/file.mtar";
    private static final String URL_WITH_CREDENTIALS = "https://user:pass@example.com/file.mtar";
    private static final String JOB_ID = "job-123";
    private static final String MTA_ID = "test-mta";
    private static final String FILE_ID = "file-123";
    private static final String USERNAME = "test-user";
    private static final String INVALID_FILE_URL = "https://example.com/file_without_extension";
    private static final String ERROR_MESSAGE = "Test error message";
    private static final long MAX_UPLOAD_SIZE = 100L * 1024 * 1024; // 100MB
    private static final long OVERSIZED_FILE = 200L * 1024 * 1024; // 200MB

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
    private HttpClient httpClient;

    @Mock
    private HttpResponse<InputStream> httpResponse;

    @Mock
    private HttpHeaders httpHeaders;

    @Mock
    private DescriptorParserFacade descriptorParserFacade;

    @Mock
    private DeploymentDescriptor deploymentDescriptor;

    @Mock
    private UserCredentials userCredentials;

    @Mock
    private AsyncUploadJobsQuery asyncUploadJobsQuery;

    private TestableAsyncUploadJobExecutor asyncUploadJobExecutor;
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

        asyncUploadJobExecutor = new TestableAsyncUploadJobExecutor(
            asyncFileUploadExecutor,
            deployFromUrlExecutor,
            applicationConfiguration,
            asyncUploadJobService,
            fileService,
            descriptorParserFactory
        );

        when(applicationConfiguration.getApplicationInstanceIndex()).thenReturn(0);
        when(applicationConfiguration.getMaxUploadSize()).thenReturn(MAX_UPLOAD_SIZE);
        when(applicationConfiguration.getMaxMtaDescriptorSize()).thenReturn(1024L * 1024);

        when(asyncUploadJobService.createQuery()).thenReturn(asyncUploadJobsQuery);
        when(asyncUploadJobsQuery.id(anyString())).thenReturn(asyncUploadJobsQuery);

        when(descriptorParserFactory.getInstance()).thenReturn(descriptorParserFacade);
        when(deploymentDescriptor.getId()).thenReturn(MTA_ID);
        when(userCredentials.getUsername()).thenReturn("testuser");
        when(userCredentials.getPassword()).thenReturn("testpass");
    }

    @Test
    void testJobFinishesSuccessfully() throws Exception {
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

        setupSuccessfulHttpResponseMocks();

        when(fileService.addFile(any(FileEntry.class), any(InputStream.class))).thenReturn(fileEntry);
        when(fileService.processFileContent(anyString(), anyString(), any())).thenReturn(deploymentDescriptor);

        when(descriptorParserFacade.parseDeploymentDescriptor(anyString())).thenReturn(deploymentDescriptor);

        try (MockedStatic<SecurityContextUtil> mockedSecurityContext = mockStatic(SecurityContextUtil.class)) {
            mockedSecurityContext.when(SecurityContextUtil::getUsername)
                                 .thenReturn(USERNAME);

            AsyncUploadJobEntry result = asyncUploadJobExecutor.executeUploadFromUrl(
                SPACE_GUID, NAMESPACE, FILE_URL, DECODED_URL, userCredentials);

            assertNotNull(result);
            assertEquals(SPACE_GUID, result.getSpaceGuid());
            assertEquals(NAMESPACE, result.getNamespace());
            assertEquals(FILE_URL, result.getUrl());
            assertEquals(AsyncUploadJobEntry.State.INITIAL, result.getState());

            verify(asyncUploadJobService).add(any(AsyncUploadJobEntry.class));
            verify(deployFromUrlExecutor).submit(any(Runnable.class));
        }
    }

    @Test
    void testInsecureUrlThrowsException() {
        AsyncUploadJobEntry initialEntry = createInitialJobEntry();
        AsyncUploadJobEntry runningEntry = ImmutableAsyncUploadJobEntry.copyOf(initialEntry)
                                                                       .withState(AsyncUploadJobEntry.State.RUNNING)
                                                                       .withStartedAt(LocalDateTime.now());
        AsyncUploadJobEntry errorEntry = ImmutableAsyncUploadJobEntry.copyOf(runningEntry)
                                                                     .withState(AsyncUploadJobEntry.State.ERROR)
                                                                     .withError(Messages.MTAR_ENDPOINT_NOT_SECURE);

        when(asyncUploadJobService.add(any(AsyncUploadJobEntry.class))).thenReturn(initialEntry);
        when(asyncUploadJobService.update(any(AsyncUploadJobEntry.class), any(AsyncUploadJobEntry.class)))
            .thenReturn(runningEntry)
            .thenReturn(errorEntry);
        when(asyncUploadJobsQuery.singleResult()).thenReturn(runningEntry);

        try (MockedStatic<SecurityContextUtil> mockedSecurityContext = mockStatic(SecurityContextUtil.class)) {
            mockedSecurityContext.when(SecurityContextUtil::getUsername)
                                 .thenReturn(USERNAME);

            AsyncUploadJobEntry result = asyncUploadJobExecutor.executeUploadFromUrl(
                SPACE_GUID, NAMESPACE, INSECURE_URL, INSECURE_URL, userCredentials);

            assertNotNull(result);
            verify(asyncUploadJobService).add(any(AsyncUploadJobEntry.class));
            verify(deployFromUrlExecutor).submit(any(Runnable.class));
            verify(asyncUploadJobService, times(3)).update(any(AsyncUploadJobEntry.class), any(AsyncUploadJobEntry.class));
        }
    }

    @Test
    void testFileSizeExceedsMaxUploadSize() throws Exception {
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

        setupOversizedFileHttpResponseMocks();

        try (MockedStatic<SecurityContextUtil> mockedSecurityContext = mockStatic(SecurityContextUtil.class)) {
            mockedSecurityContext.when(SecurityContextUtil::getUsername)
                                 .thenReturn(USERNAME);

            AsyncUploadJobEntry result = asyncUploadJobExecutor.executeUploadFromUrl(
                SPACE_GUID, NAMESPACE, FILE_URL, DECODED_URL, userCredentials);

            assertNotNull(result);
            verify(asyncUploadJobService).add(any(AsyncUploadJobEntry.class));
            verify(deployFromUrlExecutor).submit(any(Runnable.class));
            verify(asyncUploadJobService, times(3)).update(any(AsyncUploadJobEntry.class), any(AsyncUploadJobEntry.class));
        }
    }

    @Test
    void testInvalidFileExtension() throws Exception {
        AsyncUploadJobEntry initialEntry = createInitialJobEntry();
        AsyncUploadJobEntry runningEntry = ImmutableAsyncUploadJobEntry.copyOf(initialEntry)
                                                                       .withState(AsyncUploadJobEntry.State.RUNNING)
                                                                       .withStartedAt(LocalDateTime.now());
        AsyncUploadJobEntry errorEntry = ImmutableAsyncUploadJobEntry.copyOf(runningEntry)
                                                                     .withState(AsyncUploadJobEntry.State.ERROR)
                                                                     .withError("Invalid file extension");

        when(asyncUploadJobService.add(any(AsyncUploadJobEntry.class))).thenReturn(initialEntry);
        when(asyncUploadJobService.update(any(AsyncUploadJobEntry.class), any(AsyncUploadJobEntry.class)))
            .thenReturn(runningEntry)
            .thenReturn(errorEntry);
        when(asyncUploadJobsQuery.singleResult()).thenReturn(runningEntry);

        setupSuccessfulHttpResponseMocks();

        try (MockedStatic<SecurityContextUtil> mockedSecurityContext = mockStatic(SecurityContextUtil.class)) {
            mockedSecurityContext.when(SecurityContextUtil::getUsername)
                                 .thenReturn(USERNAME);

            AsyncUploadJobEntry result = asyncUploadJobExecutor.executeUploadFromUrl(
                SPACE_GUID, NAMESPACE, INVALID_FILE_URL, INVALID_FILE_URL, userCredentials);

            assertNotNull(result);
            verify(asyncUploadJobService).add(any(AsyncUploadJobEntry.class));
            verify(deployFromUrlExecutor).submit(any(Runnable.class));
            verify(asyncUploadJobService, times(3)).update(any(AsyncUploadJobEntry.class), any(AsyncUploadJobEntry.class));
        }
    }

    @Test
    void testHttpUnauthorizedError() throws Exception {
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

        setupUnauthorizedHttpResponseMocks();

        try (MockedStatic<SecurityContextUtil> mockedSecurityContext = mockStatic(SecurityContextUtil.class)) {
            mockedSecurityContext.when(SecurityContextUtil::getUsername)
                                 .thenReturn(USERNAME);

            AsyncUploadJobEntry result = asyncUploadJobExecutor.executeUploadFromUrl(
                SPACE_GUID, NAMESPACE, FILE_URL, DECODED_URL, userCredentials);

            assertNotNull(result);
            verify(asyncUploadJobService).add(any(AsyncUploadJobEntry.class));
            verify(deployFromUrlExecutor).submit(any(Runnable.class));
            verify(asyncUploadJobService, times(3)).update(any(AsyncUploadJobEntry.class), any(AsyncUploadJobEntry.class));
        }
    }

    @Test
    void testHttpServerError() throws Exception {
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

        setupServerErrorHttpResponseMocks();

        try (MockedStatic<SecurityContextUtil> mockedSecurityContext = mockStatic(SecurityContextUtil.class)) {
            mockedSecurityContext.when(SecurityContextUtil::getUsername)
                                 .thenReturn(USERNAME);

            AsyncUploadJobEntry result = asyncUploadJobExecutor.executeUploadFromUrl(
                SPACE_GUID, NAMESPACE, FILE_URL, DECODED_URL, userCredentials);

            assertNotNull(result);
            verify(asyncUploadJobService).add(any(AsyncUploadJobEntry.class));
            verify(deployFromUrlExecutor).submit(any(Runnable.class));
            verify(asyncUploadJobService, times(3)).update(any(AsyncUploadJobEntry.class), any(AsyncUploadJobEntry.class));
        }
    }

    @Test
    void testMissingContentLengthHeader() throws Exception {
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

        setupHttpResponseWithoutContentLength();

        try (MockedStatic<SecurityContextUtil> mockedSecurityContext = mockStatic(SecurityContextUtil.class)) {
            mockedSecurityContext.when(SecurityContextUtil::getUsername)
                                 .thenReturn(USERNAME);

            AsyncUploadJobEntry result = asyncUploadJobExecutor.executeUploadFromUrl(
                SPACE_GUID, NAMESPACE, FILE_URL, DECODED_URL, userCredentials);

            assertNotNull(result);
            verify(asyncUploadJobService).add(any(AsyncUploadJobEntry.class));
            verify(deployFromUrlExecutor).submit(any(Runnable.class));
            verify(asyncUploadJobService, times(3)).update(any(AsyncUploadJobEntry.class), any(AsyncUploadJobEntry.class));
        }
    }

    @Test
    void testWithUserCredentialsInUrl() throws Exception {
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

        setupSuccessfulHttpResponseMocks();
        when(fileService.addFile(any(FileEntry.class), any(InputStream.class))).thenReturn(fileEntry);
        when(fileService.processFileContent(anyString(), anyString(), any())).thenReturn(deploymentDescriptor);
        when(descriptorParserFacade.parseDeploymentDescriptor(anyString())).thenReturn(deploymentDescriptor);

        try (MockedStatic<SecurityContextUtil> mockedSecurityContext = mockStatic(SecurityContextUtil.class)) {
            mockedSecurityContext.when(SecurityContextUtil::getUsername)
                                 .thenReturn(USERNAME);

            AsyncUploadJobEntry result = asyncUploadJobExecutor.executeUploadFromUrl(
                SPACE_GUID, NAMESPACE, FILE_URL, URL_WITH_CREDENTIALS, null);

            assertNotNull(result);
            verify(asyncUploadJobService).add(any(AsyncUploadJobEntry.class));
            verify(deployFromUrlExecutor).submit(any(Runnable.class));
            verify(asyncUploadJobService, times(3)).update(any(AsyncUploadJobEntry.class), any(AsyncUploadJobEntry.class));
        }
    }

    @Test
    void testFileServiceException() throws Exception {
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

        setupSuccessfulHttpResponseMocks();

        when(fileService.addFile(any(FileEntry.class), any(InputStream.class)))
            .thenThrow(new RuntimeException(ERROR_MESSAGE));

        try (MockedStatic<SecurityContextUtil> mockedSecurityContext = mockStatic(SecurityContextUtil.class)) {
            mockedSecurityContext.when(SecurityContextUtil::getUsername)
                                 .thenReturn(USERNAME);

            AsyncUploadJobEntry result = asyncUploadJobExecutor.executeUploadFromUrl(
                SPACE_GUID, NAMESPACE, FILE_URL, DECODED_URL, userCredentials);

            assertNotNull(result);
            verify(asyncUploadJobService).add(any(AsyncUploadJobEntry.class));
            verify(deployFromUrlExecutor).submit(any(Runnable.class));
            verify(asyncUploadJobService, times(4)).update(any(AsyncUploadJobEntry.class), any(AsyncUploadJobEntry.class));
        }
    }

    @Test
    void testDescriptorParsingException() throws Exception {
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

        setupSuccessfulHttpResponseMocks();
        when(fileService.addFile(any(FileEntry.class), any(InputStream.class))).thenReturn(fileEntry);

        when(fileService.processFileContent(anyString(), anyString(), any()))
            .thenThrow(new RuntimeException(ERROR_MESSAGE));

        try (MockedStatic<SecurityContextUtil> mockedSecurityContext = mockStatic(SecurityContextUtil.class)) {
            mockedSecurityContext.when(SecurityContextUtil::getUsername)
                                 .thenReturn(USERNAME);

            AsyncUploadJobEntry result = asyncUploadJobExecutor.executeUploadFromUrl(
                SPACE_GUID, NAMESPACE, FILE_URL, DECODED_URL, userCredentials);

            assertNotNull(result);
            verify(asyncUploadJobService).add(any(AsyncUploadJobEntry.class));
            verify(deployFromUrlExecutor).submit(any(Runnable.class));
            verify(asyncUploadJobService, times(4)).update(any(AsyncUploadJobEntry.class), any(AsyncUploadJobEntry.class));
        }
    }

    @Test
    void testExtractDeploymentDescriptorSuccess() throws Exception {
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

        setupSuccessfulHttpResponseMocks();
        when(fileService.addFile(any(FileEntry.class), any(InputStream.class))).thenReturn(fileEntry);
        when(fileService.processFileContent(anyString(), anyString(), any())).thenReturn(deploymentDescriptor);
        when(descriptorParserFacade.parseDeploymentDescriptor(anyString())).thenReturn(deploymentDescriptor);

        try (MockedStatic<SecurityContextUtil> mockedSecurityContext = mockStatic(SecurityContextUtil.class)) {
            mockedSecurityContext.when(SecurityContextUtil::getUsername)
                                 .thenReturn(USERNAME);

            AsyncUploadJobEntry result = asyncUploadJobExecutor.executeUploadFromUrl(
                SPACE_GUID, NAMESPACE, FILE_URL, DECODED_URL, userCredentials);

            assertNotNull(result);
            verify(fileService).processFileContent(anyString(), anyString(), any());
            verify(asyncUploadJobService, times(3)).update(any(AsyncUploadJobEntry.class), any(AsyncUploadJobEntry.class));
        }
    }

    @Test
    void testMonitorAsyncUploadJobWithMultipleUpdates() throws Exception {
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

        setupSuccessfulHttpResponseMocks();
        when(fileService.addFile(any(FileEntry.class), any(InputStream.class))).thenReturn(fileEntry);
        when(fileService.processFileContent(anyString(), anyString(), any())).thenReturn(deploymentDescriptor);
        when(descriptorParserFacade.parseDeploymentDescriptor(anyString())).thenReturn(deploymentDescriptor);

        try (MockedStatic<SecurityContextUtil> mockedSecurityContext = mockStatic(SecurityContextUtil.class)) {
            mockedSecurityContext.when(SecurityContextUtil::getUsername)
                                 .thenReturn(USERNAME);

            AsyncUploadJobEntry result = asyncUploadJobExecutor.executeUploadFromUrl(
                SPACE_GUID, NAMESPACE, FILE_URL, DECODED_URL, userCredentials);

            assertNotNull(result);
            verify(asyncUploadJobService, times(4)).update(any(AsyncUploadJobEntry.class), any(AsyncUploadJobEntry.class));
            verify(asyncUploadJobsQuery, times(4)).singleResult();
        }
    }

    @Test
    void testMonitorAsyncUploadJobWithErrorState() throws Exception {
        AsyncUploadJobEntry initialEntry = createInitialJobEntry();
        AsyncUploadJobEntry runningEntry = ImmutableAsyncUploadJobEntry.copyOf(initialEntry)
                                                                       .withState(AsyncUploadJobEntry.State.RUNNING)
                                                                       .withStartedAt(LocalDateTime.now());
        AsyncUploadJobEntry errorEntry = ImmutableAsyncUploadJobEntry.copyOf(runningEntry)
                                                                     .withState(AsyncUploadJobEntry.State.ERROR)
                                                                     .withError(ERROR_MESSAGE);

        when(asyncUploadJobService.add(any(AsyncUploadJobEntry.class))).thenReturn(initialEntry);
        when(asyncUploadJobService.update(any(AsyncUploadJobEntry.class), any(AsyncUploadJobEntry.class)))
            .thenReturn(runningEntry)
            .thenReturn(runningEntry)
            .thenReturn(errorEntry);
        when(asyncUploadJobsQuery.singleResult())
            .thenReturn(runningEntry)
            .thenReturn(errorEntry);

        setupSuccessfulHttpResponseMocks();
        when(fileService.addFile(any(FileEntry.class), any(InputStream.class)))
            .thenThrow(new RuntimeException(ERROR_MESSAGE));

        try (MockedStatic<SecurityContextUtil> mockedSecurityContext = mockStatic(SecurityContextUtil.class)) {
            mockedSecurityContext.when(SecurityContextUtil::getUsername)
                                 .thenReturn(USERNAME);

            AsyncUploadJobEntry result = asyncUploadJobExecutor.executeUploadFromUrl(
                SPACE_GUID, NAMESPACE, FILE_URL, DECODED_URL, userCredentials);

            assertNotNull(result);
            verify(asyncUploadJobService, times(4)).update(any(AsyncUploadJobEntry.class), any(AsyncUploadJobEntry.class));
        }
    }

    @Test
    void testArchiveHandlerCallInExtractDeploymentDescriptor() throws Exception {
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

        setupSuccessfulHttpResponseMocks();
        when(fileService.addFile(any(FileEntry.class), any(InputStream.class))).thenReturn(fileEntry);
        when(fileService.processFileContent(anyString(), anyString(), any())).thenReturn(deploymentDescriptor);
        when(descriptorParserFacade.parseDeploymentDescriptor(anyString())).thenReturn(deploymentDescriptor);

        try (MockedStatic<SecurityContextUtil> mockedSecurityContext = mockStatic(SecurityContextUtil.class)) {
            mockedSecurityContext.when(SecurityContextUtil::getUsername)
                                 .thenReturn(USERNAME);

            AsyncUploadJobEntry result = asyncUploadJobExecutor.executeUploadFromUrl(
                SPACE_GUID, NAMESPACE, FILE_URL, DECODED_URL, userCredentials);

            assertNotNull(result);
            verify(fileService).processFileContent(anyString(), anyString(), any());
            verify(deploymentDescriptor).getId();
            verify(asyncUploadJobService, times(3)).update(any(AsyncUploadJobEntry.class), any(AsyncUploadJobEntry.class));
        }
    }

    @Test
    void testMonitorAsyncUploadJobBytesReadProgression() throws Exception {
        AsyncUploadJobEntry initialEntry = createInitialJobEntry();
        AsyncUploadJobEntry runningEntry1 = ImmutableAsyncUploadJobEntry.copyOf(initialEntry)
                                                                        .withState(AsyncUploadJobEntry.State.RUNNING)
                                                                        .withStartedAt(LocalDateTime.now())
                                                                        .withBytesRead(0L);
        AsyncUploadJobEntry runningEntry2 = ImmutableAsyncUploadJobEntry.copyOf(runningEntry1)
                                                                        .withBytesRead(512L);
        AsyncUploadJobEntry finishedEntry = ImmutableAsyncUploadJobEntry.copyOf(runningEntry2)
                                                                        .withState(AsyncUploadJobEntry.State.FINISHED)
                                                                        .withFileId(FILE_ID)
                                                                        .withMtaId(MTA_ID)
                                                                        .withBytesRead(1024L)
                                                                        .withFinishedAt(LocalDateTime.now());

        FileEntry fileEntry = createFileEntry();

        when(asyncUploadJobService.add(any(AsyncUploadJobEntry.class))).thenReturn(initialEntry);
        when(asyncUploadJobService.update(any(AsyncUploadJobEntry.class), any(AsyncUploadJobEntry.class)))
            .thenReturn(runningEntry1)
            .thenReturn(runningEntry2)
            .thenReturn(finishedEntry);

        when(asyncUploadJobsQuery.singleResult())
            .thenReturn(runningEntry1)
            .thenReturn(runningEntry2)
            .thenReturn(finishedEntry);

        setupSuccessfulHttpResponseMocks();
        when(fileService.addFile(any(FileEntry.class), any(InputStream.class))).thenReturn(fileEntry);
        when(fileService.processFileContent(anyString(), anyString(), any())).thenReturn(deploymentDescriptor);
        when(descriptorParserFacade.parseDeploymentDescriptor(anyString())).thenReturn(deploymentDescriptor);

        try (MockedStatic<SecurityContextUtil> mockedSecurityContext = mockStatic(SecurityContextUtil.class)) {
            mockedSecurityContext.when(SecurityContextUtil::getUsername)
                                 .thenReturn(USERNAME);

            AsyncUploadJobEntry result = asyncUploadJobExecutor.executeUploadFromUrl(
                SPACE_GUID, NAMESPACE, FILE_URL, DECODED_URL, userCredentials);

            assertNotNull(result);
            verify(asyncUploadJobService, times(3)).update(any(AsyncUploadJobEntry.class), any(AsyncUploadJobEntry.class));
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
                                 .size(BigInteger.valueOf(1024))
                                 .build();
    }

    @SuppressWarnings("unchecked")
    private void setupSuccessfulHttpResponseMocks() throws Exception {
        String mtarContent = """
            _schema-version: 3.3.0
            ID: test-mta
            version: 1.0.0
            """;

        ByteArrayInputStream inputStream = new ByteArrayInputStream(mtarContent.getBytes());

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(inputStream);
        when(httpResponse.headers()).thenReturn(httpHeaders);
        when(httpResponse.uri()).thenReturn(URI.create(FILE_URL));
        when(httpHeaders.firstValueAsLong(Constants.CONTENT_LENGTH))
            .thenReturn(OptionalLong.of(1024L));
    }

    @SuppressWarnings("unchecked")
    private void setupOversizedFileHttpResponseMocks() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.headers()).thenReturn(httpHeaders);
        when(httpResponse.uri()).thenReturn(URI.create(FILE_URL));
        when(httpHeaders.firstValueAsLong(Constants.CONTENT_LENGTH))
            .thenReturn(OptionalLong.of(OVERSIZED_FILE));
    }

    @SuppressWarnings("unchecked")
    private void setupUnauthorizedHttpResponseMocks() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(HttpStatus.UNAUTHORIZED.value());
        when(httpResponse.body()).thenReturn(new ByteArrayInputStream("Unauthorized".getBytes()));
        when(httpResponse.uri()).thenReturn(URI.create(FILE_URL));
    }

    @SuppressWarnings("unchecked")
    private void setupServerErrorHttpResponseMocks() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR.value());
        when(httpResponse.body()).thenReturn(new ByteArrayInputStream("Server Error".getBytes()));
        when(httpResponse.uri()).thenReturn(URI.create(FILE_URL));
    }

    @SuppressWarnings("unchecked")
    private void setupHttpResponseWithoutContentLength() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.headers()).thenReturn(httpHeaders);
        when(httpResponse.uri()).thenReturn(URI.create(FILE_URL));
        when(httpHeaders.firstValueAsLong(Constants.CONTENT_LENGTH))
            .thenReturn(OptionalLong.empty());
    }

    private class TestableAsyncUploadJobExecutor extends AsyncUploadJobExecutor {

        public TestableAsyncUploadJobExecutor(ExecutorService asyncFileUploadExecutor,
                                              ExecutorService deployFromUrlExecutor,
                                              ApplicationConfiguration applicationConfiguration,
                                              AsyncUploadJobService asyncUploadJobService,
                                              FileService fileService,
                                              DescriptorParserFacadeFactory descriptorParserFactory) {
            super(asyncFileUploadExecutor, deployFromUrlExecutor, applicationConfiguration,
                  asyncUploadJobService, fileService, descriptorParserFactory);
        }

        @Override
        protected HttpClient buildHttpClient() {
            return httpClient;
        }

        @Override
        protected ResilientOperationExecutor getResilientOperationExecutor() {
            return new ResilientOperationExecutor() {
                @Override
                public <T> T execute(org.cloudfoundry.multiapps.controller.client.util.CheckedSupplier<T> operation) throws Exception {
                    return operation.get();
                }
            };
        }
    }
}
