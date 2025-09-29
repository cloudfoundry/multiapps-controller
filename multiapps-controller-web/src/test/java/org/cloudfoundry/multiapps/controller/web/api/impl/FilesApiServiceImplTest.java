package org.cloudfoundry.multiapps.controller.web.api.impl;

import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import org.apache.commons.lang3.RandomStringUtils;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.api.model.AsyncUploadResult;
import org.cloudfoundry.multiapps.controller.api.model.FileMetadata;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableFileUrl;
import org.cloudfoundry.multiapps.controller.core.auditlogging.FilesApiServiceAuditLog;
import org.cloudfoundry.multiapps.controller.core.util.UserInfo;
import org.cloudfoundry.multiapps.controller.persistence.Constants;
import org.cloudfoundry.multiapps.controller.persistence.model.AsyncUploadJobEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.AsyncUploadJobEntry.State;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableAsyncUploadJobEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableFileEntry;
import org.cloudfoundry.multiapps.controller.persistence.query.AsyncUploadJobsQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.AsyncUploadJobService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.web.upload.AsyncUploadJobOrchestrator;
import org.cloudfoundry.multiapps.controller.web.upload.exception.RejectedAsyncUploadJobException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FilesApiServiceImplTest {

    private static final String SPACE_GUID = "896e6be9-8217-4a1c-b938-09b30966157a";
    private static final String NAMESPACE = "custom-namespace";
    private static final String DIGEST_CHARACTER_TABLE = "123456789ABCDEF";

    private static final String FILE_URL = Base64.getUrlEncoder()
                                                 .encodeToString(
                                                     "https://host.domain/test.mtar?query=true".getBytes(StandardCharsets.UTF_8));
    private static final String DECODED_URL_WITH_CREDENTIALS_IN_THE_URL = "https://abc:abv@google.com";

    @Mock
    private FileService fileService;
    @Mock
    private MultipartHttpServletRequest request;
    @Mock
    private MultipartFile file;
    @InjectMocks
    private final FilesApiServiceImpl testedClass = new FilesApiServiceImpl();
    @Mock
    private FilesApiServiceAuditLog filesApiServiceAuditLog;
    @Mock(name = "fileStorageThreadPool")
    private ExecutorService fileStorageThreadPool;
    @Mock
    private AsyncUploadJobOrchestrator asyncUploadJobOrchestrator;
    @Mock
    private AsyncUploadJobService uploadJobService;

    @BeforeEach
    public void initialize() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        SecurityContextHolder.clearContext();
        var user = new UserInfo("user1", "user1", null);
        var token = new DefaultOAuth2User(Collections.emptyList(), Map.of("user_info", user), "user_info");
        SecurityContextHolder.getContext()
                             .setAuthentication(new OAuth2AuthenticationToken(token, Collections.emptyList(), "id"));
        Mockito.when(request.getRequestURI())
               .thenReturn("");
        prepareFileStorageThreadPool();
    }

    @AfterEach
    public void cleanup() {
        SecurityContextHolder.clearContext();
    }

    private void prepareFileStorageThreadPool() {
        when(fileStorageThreadPool.submit(any(Callable.class))).thenAnswer(invocation -> {
            Callable<?> callable = invocation.getArgument(0);
            FutureTask<?> futureTask = new FutureTask<>(callable);
            futureTask.run();
            return futureTask;
        });
    }

    @Test
    void testGetMtaFiles() throws Exception {
        FileEntry entryOne = createFileEntry("test.mtar");
        FileEntry entryTwo = createFileEntry("extension.mtaext");
        Mockito.when(fileService.listFiles(Mockito.eq(SPACE_GUID), Mockito.eq(NAMESPACE)))
               .thenReturn(List.of(entryOne, entryTwo));
        ResponseEntity<List<FileMetadata>> response = testedClass.getFiles(SPACE_GUID, NAMESPACE);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<FileMetadata> files = response.getBody();
        assertEquals(2, files.size());
        assertMetadataMatches(entryOne, files.get(0));
        assertMetadataMatches(entryTwo, files.get(1));
    }

    private FileEntry createFileEntry(String name) {
        return ImmutableFileEntry.builder()
                                 .id(UUID.randomUUID()
                                         .toString())
                                 .digest(RandomStringUtils.random(32, DIGEST_CHARACTER_TABLE))
                                 .digestAlgorithm(Constants.DIGEST_ALGORITHM)
                                 .name(name)
                                 .namespace(NAMESPACE)
                                 .size(BigInteger.valueOf(new Random().nextInt(1024 * 1024 * 10)))
                                 .space(SPACE_GUID)
                                 .build();
    }

    private void assertMetadataMatches(FileEntry expected, FileMetadata actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getSpace(), actual.getSpace());
        assertEquals(expected.getSize(), actual.getSize());
        assertEquals(expected.getDigest(), actual.getDigest());
        assertEquals(expected.getDigestAlgorithm(), actual.getDigestAlgorithm());
    }

    @Test
    void testGetMtaFilesError() throws Exception {
        Mockito.when(fileService.listFiles(Mockito.eq(SPACE_GUID), Mockito.eq(null)))
               .thenThrow(new FileStorageException("error"));
        assertThrows(SLException.class, () -> testedClass.getFiles(SPACE_GUID, null));
    }

    @Test
    void testUploadMtaFile() throws Exception {
        String fileName = "test.mtar";
        FileEntry fileEntry = createFileEntry(fileName);
        long fileSize = fileEntry.getSize()
                                 .longValue();

        Mockito.when(file.getSize())
               .thenReturn(fileSize);
        Mockito.when(file.getOriginalFilename())
               .thenReturn(fileName);
        Mockito.when(file.getInputStream())
               .thenReturn(Mockito.mock(InputStream.class));
        Mockito.when(request.getFileMap())
               .thenReturn(Map.of("file", file));

        Mockito.when(fileService.addFile(Mockito.eq(ImmutableFileEntry.builder()
                                                                      .space(SPACE_GUID)
                                                                      .namespace(NAMESPACE)
                                                                      .name(fileName)
                                                                      .size(BigInteger.valueOf(fileSize))
                                                                      .build()), any(InputStream.class)))
               .thenReturn(fileEntry);

        ResponseEntity<FileMetadata> response = testedClass.uploadFile(request, SPACE_GUID, NAMESPACE);

        Mockito.verify(file)
               .getInputStream();
        Mockito.verify(fileService)
               .addFile(Mockito.eq(ImmutableFileEntry.builder()
                                                     .space(SPACE_GUID)
                                                     .namespace(NAMESPACE)
                                                     .name(fileName)
                                                     .size(BigInteger.valueOf(fileSize))
                                                     .build()), any(InputStream.class));

        FileMetadata fileMetadata = response.getBody();
        assertMetadataMatches(fileEntry, fileMetadata);
    }

    @Test
    void testStartDeployFromUrl() {
        AsyncUploadJobsQuery asyncUploadJobsQuery = mock(AsyncUploadJobsQuery.class, RETURNS_SELF);
        when(uploadJobService.createQuery()).thenReturn(asyncUploadJobsQuery);
        when(asyncUploadJobsQuery.list()).thenReturn(Collections.emptyList());

        String expectedJobId = UUID.randomUUID()
                                   .toString();
        when(asyncUploadJobOrchestrator.executeUploadFromUrl(any(), any(), any(), any(), any())).thenReturn(
            ImmutableAsyncUploadJobEntry.builder()
                                        .id(expectedJobId)
                                        .url(DECODED_URL_WITH_CREDENTIALS_IN_THE_URL)
                                        .startedAt(LocalDateTime.now())
                                        .state(State.INITIAL)
                                        .user("user1")
                                        .spaceGuid(SPACE_GUID)
                                        .instanceIndex(0)
                                        .build());
        ResponseEntity<Void> response = testedClass.startUploadFromUrl(SPACE_GUID, NAMESPACE, ImmutableFileUrl.builder()
                                                                                                              .fileUrl(FILE_URL)
                                                                                                              .build());
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals("spaces/" + SPACE_GUID + "/files/jobs/" + expectedJobId, response.getHeaders()
                                                                                      .getLocation()
                                                                                      .toString());
        Mockito.verify(asyncUploadJobOrchestrator)
               .executeUploadFromUrl(eq(SPACE_GUID), eq(NAMESPACE), any(String.class), any(String.class), eq(null));
    }

    @Test
    void testStartDeployFromUrlWhenExecutorRejectsJob() {
        AsyncUploadJobsQuery asyncUploadJobsQuery = mock(AsyncUploadJobsQuery.class, RETURNS_SELF);
        when(uploadJobService.createQuery()).thenReturn(asyncUploadJobsQuery);
        when(asyncUploadJobsQuery.list()).thenReturn(Collections.emptyList());

        AsyncUploadJobEntry rejectedEntry = mock(AsyncUploadJobEntry.class);
        when(rejectedEntry.getId()).thenReturn("rejected-job-id");

        when(asyncUploadJobOrchestrator.executeUploadFromUrl(any(), any(), any(), any(), any())).thenThrow(
            new RejectedAsyncUploadJobException(rejectedEntry));

        ResponseEntity<Void> response = testedClass.startUploadFromUrl(SPACE_GUID, NAMESPACE, ImmutableFileUrl.builder()
                                                                                                              .fileUrl(FILE_URL)
                                                                                                              .build());

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("30", response.getHeaders()
                                   .getFirst("Retry-After"));

        Mockito.verify(asyncUploadJobOrchestrator)
               .executeUploadFromUrl(eq(SPACE_GUID), eq(NAMESPACE), any(String.class), any(String.class), eq(null));

        Mockito.verify(asyncUploadJobsQuery)
               .delete();
    }

    @Test
    void testStartDeployFromUrlWhenStuckJobExists() {
        AsyncUploadJobsQuery asyncUploadJobsQuery = mock(AsyncUploadJobsQuery.class, RETURNS_SELF);
        when(uploadJobService.createQuery()).thenReturn(asyncUploadJobsQuery);

        String existingJobId = UUID.randomUUID()
                                   .toString();
        AsyncUploadJobEntry stuckJob = ImmutableAsyncUploadJobEntry.builder()
                                                                   .id(existingJobId)
                                                                   .url(DECODED_URL_WITH_CREDENTIALS_IN_THE_URL)
                                                                   .startedAt(LocalDateTime.now()
                                                                                           .minusMinutes(5))
                                                                   .updatedAt(LocalDateTime.now()
                                                                                           .minusMinutes(2))
                                                                   .state(State.RUNNING)
                                                                   .user("user1")
                                                                   .spaceGuid(SPACE_GUID)
                                                                   .instanceIndex(0)
                                                                   .build();

        when(asyncUploadJobsQuery.list()).thenReturn(List.of(stuckJob))
                                         .thenReturn(Collections.emptyList());

        String newJobId = UUID.randomUUID()
                              .toString();
        when(asyncUploadJobOrchestrator.executeUploadFromUrl(any(), any(), any(), any(), any())).thenReturn(
            ImmutableAsyncUploadJobEntry.builder()
                                        .id(newJobId)
                                        .url(DECODED_URL_WITH_CREDENTIALS_IN_THE_URL)
                                        .startedAt(LocalDateTime.now())
                                        .state(State.INITIAL)
                                        .user("user1")
                                        .spaceGuid(SPACE_GUID)
                                        .instanceIndex(0)
                                        .build());

        ResponseEntity<Void> response = testedClass.startUploadFromUrl(SPACE_GUID, NAMESPACE, ImmutableFileUrl.builder()
                                                                                                              .fileUrl(FILE_URL)
                                                                                                              .build());

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals("spaces/" + SPACE_GUID + "/files/jobs/" + newJobId, response.getHeaders()
                                                                                 .getLocation()
                                                                                 .toString());

        Mockito.verify(asyncUploadJobsQuery)
               .id(existingJobId);
        Mockito.verify(asyncUploadJobsQuery)
               .delete();
        Mockito.verify(asyncUploadJobOrchestrator)
               .executeUploadFromUrl(eq(SPACE_GUID), eq(NAMESPACE), any(String.class), any(String.class), eq(null));
    }

    @Test
    void testStartDeployFromUrlWhenActiveJobExists() {
        AsyncUploadJobsQuery asyncUploadJobsQuery = mock(AsyncUploadJobsQuery.class, RETURNS_SELF);
        when(uploadJobService.createQuery()).thenReturn(asyncUploadJobsQuery);

        String existingJobId = UUID.randomUUID()
                                   .toString();
        AsyncUploadJobEntry activeJob = ImmutableAsyncUploadJobEntry.builder()
                                                                    .id(existingJobId)
                                                                    .url(DECODED_URL_WITH_CREDENTIALS_IN_THE_URL)
                                                                    .startedAt(LocalDateTime.now()
                                                                                            .minusMinutes(1))
                                                                    .updatedAt(LocalDateTime.now()
                                                                                            .minusSeconds(10))
                                                                    .state(State.RUNNING)
                                                                    .user("user1")
                                                                    .spaceGuid(SPACE_GUID)
                                                                    .instanceIndex(0)
                                                                    .build();

        when(asyncUploadJobsQuery.list()).thenReturn(List.of(activeJob));

        ResponseEntity<Void> response = testedClass.startUploadFromUrl(SPACE_GUID, NAMESPACE, ImmutableFileUrl.builder()
                                                                                                              .fileUrl(FILE_URL)
                                                                                                              .build());

        assertEquals(HttpStatus.SEE_OTHER, response.getStatusCode());
        assertEquals("spaces/" + SPACE_GUID + "/files/jobs/" + existingJobId, response.getHeaders()
                                                                                      .getLocation()
                                                                                      .toString());

        Mockito.verify(asyncUploadJobsQuery, Mockito.never())
               .id(any());
        Mockito.verify(asyncUploadJobsQuery, Mockito.never())
               .delete();
        Mockito.verify(asyncUploadJobOrchestrator, Mockito.never())
               .executeUploadFromUrl(any(), any(), any(), any(), any());
    }

    @Test
    void testGetUploadFromUrlJobWhenJobNotFound() {
        AsyncUploadJobsQuery asyncUploadJobsQuery = mock(AsyncUploadJobsQuery.class, RETURNS_SELF);
        when(uploadJobService.createQuery()).thenReturn(asyncUploadJobsQuery);
        when(asyncUploadJobsQuery.list()).thenReturn(Collections.emptyList());

        ResponseEntity<AsyncUploadResult> response = testedClass.getUploadFromUrlJob(SPACE_GUID, NAMESPACE, "non-existent-job-id");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        Mockito.verify(asyncUploadJobsQuery)
               .id("non-existent-job-id");
        Mockito.verify(asyncUploadJobsQuery)
               .spaceGuid(SPACE_GUID);
        Mockito.verify(asyncUploadJobsQuery)
               .user("user1");
        Mockito.verify(asyncUploadJobsQuery)
               .namespace(NAMESPACE);
    }

    @Test
    void testGetUploadFromUrlJobWhenJobIsSuccessful() throws FileStorageException {
        String jobId = UUID.randomUUID()
                           .toString();
        String fileId = UUID.randomUUID()
                            .toString();
        String mtaId = "test-mta";

        AsyncUploadJobEntry successfulJob = ImmutableAsyncUploadJobEntry.builder()
                                                                        .id(jobId)
                                                                        .url(DECODED_URL_WITH_CREDENTIALS_IN_THE_URL)
                                                                        .startedAt(LocalDateTime.now()
                                                                                                .minusMinutes(5))
                                                                        .updatedAt(LocalDateTime.now()
                                                                                                .minusMinutes(1))
                                                                        .state(State.FINISHED)
                                                                        .user("user1")
                                                                        .spaceGuid(SPACE_GUID)
                                                                        .instanceIndex(0)
                                                                        .fileId(fileId)
                                                                        .mtaId(mtaId)
                                                                        .build();

        FileEntry fileEntry = createFileEntry("test.mtar");

        AsyncUploadJobsQuery asyncUploadJobsQuery = mock(AsyncUploadJobsQuery.class, RETURNS_SELF);
        when(uploadJobService.createQuery()).thenReturn(asyncUploadJobsQuery);
        when(asyncUploadJobsQuery.list()).thenReturn(List.of(successfulJob));
        when(fileService.getFile(SPACE_GUID, fileId)).thenReturn(fileEntry);

        ResponseEntity<AsyncUploadResult> response = testedClass.getUploadFromUrlJob(SPACE_GUID, NAMESPACE, jobId);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        AsyncUploadResult result = response.getBody();
        assertEquals(AsyncUploadResult.JobStatus.FINISHED, result.getStatus());
        assertEquals(mtaId, result.getMtaId());
        assertEquals(fileEntry.getId(), result.getFile()
                                              .getId());
        assertEquals(fileEntry.getName(), result.getFile()
                                                .getName());
    }

    @Test
    void testGetUploadFromUrlJobWhenJobIsStuck() {
        String jobId = UUID.randomUUID()
                           .toString();

        AsyncUploadJobEntry stuckJob = ImmutableAsyncUploadJobEntry.builder()
                                                                   .id(jobId)
                                                                   .url(DECODED_URL_WITH_CREDENTIALS_IN_THE_URL)
                                                                   .startedAt(LocalDateTime.now()
                                                                                           .minusMinutes(5))
                                                                   .updatedAt(LocalDateTime.now()
                                                                                           .minusMinutes(2))
                                                                   .state(State.RUNNING)
                                                                   .user("user1")
                                                                   .spaceGuid(SPACE_GUID)
                                                                   .instanceIndex(0)
                                                                   .build();

        AsyncUploadJobsQuery asyncUploadJobsQuery = mock(AsyncUploadJobsQuery.class, RETURNS_SELF);
        when(uploadJobService.createQuery()).thenReturn(asyncUploadJobsQuery);
        when(asyncUploadJobsQuery.list()).thenReturn(List.of(stuckJob));

        ResponseEntity<AsyncUploadResult> response = testedClass.getUploadFromUrlJob(SPACE_GUID, NAMESPACE, jobId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        AsyncUploadResult result = response.getBody();
        assertEquals(AsyncUploadResult.JobStatus.ERROR, result.getStatus());
        assertEquals(List.of(AsyncUploadResult.ClientAction.RETRY_UPLOAD), result.getClientActions());
        assertTrue(result.getError()
                         .contains("30"));
    }

    @Test
    void testGetUploadFromUrlJobWhenJobIsRunning() {
        String jobId = UUID.randomUUID()
                           .toString();
        Long bytesRead = 1024L;

        AsyncUploadJobEntry runningJob = ImmutableAsyncUploadJobEntry.builder()
                                                                     .id(jobId)
                                                                     .url(DECODED_URL_WITH_CREDENTIALS_IN_THE_URL)
                                                                     .startedAt(LocalDateTime.now()
                                                                                             .minusMinutes(1))
                                                                     .updatedAt(LocalDateTime.now()
                                                                                             .minusSeconds(10))
                                                                     .state(State.RUNNING)
                                                                     .user("user1")
                                                                     .spaceGuid(SPACE_GUID)
                                                                     .instanceIndex(0)
                                                                     .bytesRead(bytesRead)
                                                                     .build();

        AsyncUploadJobsQuery asyncUploadJobsQuery = mock(AsyncUploadJobsQuery.class, RETURNS_SELF);
        when(uploadJobService.createQuery()).thenReturn(asyncUploadJobsQuery);
        when(asyncUploadJobsQuery.list()).thenReturn(List.of(runningJob));

        ResponseEntity<AsyncUploadResult> response = testedClass.getUploadFromUrlJob(SPACE_GUID, NAMESPACE, jobId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        AsyncUploadResult result = response.getBody();
        assertEquals(AsyncUploadResult.JobStatus.RUNNING, result.getStatus());
        assertEquals(bytesRead, result.getBytes());
    }

    @Test
    void testGetUploadFromUrlJobWhenJobIsInErrorState() {
        String jobId = UUID.randomUUID()
                           .toString();
        String errorMessage = "Download failed due to network error";

        AsyncUploadJobEntry errorJob = ImmutableAsyncUploadJobEntry.builder()
                                                                   .id(jobId)
                                                                   .url(DECODED_URL_WITH_CREDENTIALS_IN_THE_URL)
                                                                   .startedAt(LocalDateTime.now()
                                                                                           .minusMinutes(5))
                                                                   .updatedAt(LocalDateTime.now()
                                                                                           .minusMinutes(1))
                                                                   .state(State.ERROR)
                                                                   .user("user1")
                                                                   .spaceGuid(SPACE_GUID)
                                                                   .instanceIndex(0)
                                                                   .error(errorMessage)
                                                                   .build();

        AsyncUploadJobsQuery asyncUploadJobsQuery = mock(AsyncUploadJobsQuery.class, RETURNS_SELF);
        when(uploadJobService.createQuery()).thenReturn(asyncUploadJobsQuery);
        when(asyncUploadJobsQuery.list()).thenReturn(List.of(errorJob));

        ResponseEntity<AsyncUploadResult> response = testedClass.getUploadFromUrlJob(SPACE_GUID, NAMESPACE, jobId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        AsyncUploadResult result = response.getBody();
        assertEquals(AsyncUploadResult.JobStatus.ERROR, result.getStatus());
        assertEquals(errorMessage, result.getError());
    }
}
