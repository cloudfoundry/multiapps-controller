package org.cloudfoundry.multiapps.controller.web.api.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.math.BigInteger;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executor;

import javax.persistence.NoResultException;

import org.apache.commons.lang3.RandomStringUtils;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.api.model.AsyncUploadResult;
import org.cloudfoundry.multiapps.controller.api.model.FileMetadata;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableFileUrl;
import org.cloudfoundry.multiapps.controller.client.util.ResilientOperationExecutor;
import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingFacade;
import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingProvider;
import org.cloudfoundry.multiapps.controller.core.helpers.DescriptorParserFacadeFactory;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.UserInfo;
import org.cloudfoundry.multiapps.controller.persistence.Constants;
import org.cloudfoundry.multiapps.controller.persistence.model.AsyncUploadJobEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableFileEntry;
import org.cloudfoundry.multiapps.controller.persistence.query.AsyncUploadJobsQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.AsyncUploadJobService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.persistence.util.Configuration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

class FilesApiServiceImplTest {

    @Mock
    private FileService fileService;
    @Mock
    private MultipartHttpServletRequest request;
    @Mock
    private MultipartFile file;
    @Mock
    private HttpClient httpClient;
    @Mock
    private HttpResponse<InputStream> fileUrlResponse;
    @Mock
    private Executor asyncFileUploadExecutor;
    @Mock
    private ApplicationConfiguration configuration = new ApplicationConfiguration();
    @Spy
    private DescriptorParserFacadeFactory descriptorParserFactory = new DescriptorParserFacadeFactory(configuration);
    @Mock
    private AsyncUploadJobService uploadJobService;

    private static final long MAX_PERMITTED_SIZE = new Configuration().getMaxUploadSize();
    private static final String MTA_ID = "anatz";
    private static final String FILE_URL = Base64.getUrlEncoder()
                                                 .encodeToString("https://host.domain/test.mtar?query=true".getBytes(StandardCharsets.UTF_8));

    @InjectMocks
    private final FilesApiServiceImpl testedClass = new FilesApiServiceImpl() {
        @Override
        protected HttpClient buildHttpClient(String url) {
            return httpClient;
        }

        @Override
        protected ResilientOperationExecutor getResilientOperationExecutor() {
            return new ResilientOperationExecutor().withRetryCount(0)
                                                   .withWaitTimeBetweenRetriesInMillis(0);
        }
    };

    private static final String SPACE_GUID = "896e6be9-8217-4a1c-b938-09b30966157a";
    private static final String NAMESPACE_GUID = "0a42c085-b772-4b1e-bf4d-75c463aab5f6";

    private static final String DIGEST_CHARACTER_TABLE = "123456789ABCDEF";

    @BeforeAll
    public static void setUser() {
        var user = new UserInfo("user1", "user1", null);
        var token = new DefaultOAuth2User(Collections.emptyList(), Map.of("user_info", user), "user_info");
        SecurityContextHolder.getContext()
                             .setAuthentication(new OAuth2AuthenticationToken(token, Collections.emptyList(), "id"));
    }

    @BeforeEach
    public void initialize() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        Mockito.when(request.getRequestURI())
               .thenReturn("");
        AuditLoggingProvider.setFacade(Mockito.mock(AuditLoggingFacade.class));
        Mockito.doAnswer(invocationOnMock -> {
            Runnable r = invocationOnMock.getArgument(0);
            r.run();
            return null;
        })
               .when(asyncFileUploadExecutor)
               .execute(Mockito.any());
    }

    @Test
    void testGetMtaFiles() throws Exception {
        FileEntry entryOne = createFileEntry("test.mtar");
        FileEntry entryTwo = createFileEntry("extension.mtaet");
        Mockito.when(fileService.listFiles(Mockito.eq(SPACE_GUID), Mockito.eq(NAMESPACE_GUID)))
               .thenReturn(List.of(entryOne, entryTwo));
        ResponseEntity<List<FileMetadata>> response = testedClass.getFiles(SPACE_GUID, NAMESPACE_GUID);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<FileMetadata> files = response.getBody();
        assertEquals(2, files.size());
        assertMetadataMatches(entryOne, files.get(0));
        assertMetadataMatches(entryTwo, files.get(1));

    }

    @Test
    void testGetMtaFilesError() throws Exception {
        Mockito.when(fileService.listFiles(Mockito.eq(SPACE_GUID), Mockito.eq(null)))
               .thenThrow(new FileStorageException("error"));
        Assertions.assertThrows(SLException.class, () -> testedClass.getFiles(SPACE_GUID, null));
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

        Mockito.when(fileService.addFile(Mockito.eq(SPACE_GUID), Mockito.eq(NAMESPACE_GUID), Mockito.eq(fileName),
                                         Mockito.any(InputStream.class), Mockito.eq(fileSize)))
               .thenReturn(fileEntry);

        ResponseEntity<FileMetadata> response = testedClass.uploadFile(request, SPACE_GUID, NAMESPACE_GUID);

        Mockito.verify(file)
               .getInputStream();
        Mockito.verify(fileService)
               .addFile(Mockito.eq(SPACE_GUID), Mockito.eq(NAMESPACE_GUID), Mockito.eq(fileName), Mockito.any(InputStream.class),
                        Mockito.eq(fileSize));

        FileMetadata fileMetadata = response.getBody();
        assertMetadataMatches(fileEntry, fileMetadata);
    }

    @Test
    void testUploadFileFromUrl() throws Exception {
        String fileName = "test.mtar";
        FileEntry fileEntry = createFileEntry(fileName);
        HttpHeaders headers = HttpHeaders.of(Map.of("Content-Length", List.of("20")), (a, b) -> true);
        InputStream mockStream = Mockito.mock(InputStream.class);
        AsyncUploadJobsQuery query = Mockito.mock(AsyncUploadJobsQuery.class, Answers.RETURNS_SELF);
        var jobEntry = mockUploadJobEntry(fileEntry.getId(), AsyncUploadJobEntry.State.FINISHED, null);

        Mockito.when(fileUrlResponse.headers())
               .thenReturn(headers);
        Mockito.when(fileUrlResponse.statusCode())
               .thenReturn(200);
        Mockito.when(fileUrlResponse.body())
               .thenReturn(mockStream);

        Mockito.when(httpClient.send(Mockito.any(), Mockito.eq(BodyHandlers.ofInputStream())))
               .thenReturn(fileUrlResponse);

        Mockito.when(query.singleResult())
               .thenReturn(jobEntry);
        Mockito.when(uploadJobService.createQuery())
               .thenReturn(query);
        Mockito.when(uploadJobService.update(any(), any()))
               .thenReturn(jobEntry);

        Mockito.when(fileService.addFile(Mockito.eq(SPACE_GUID), Mockito.eq(NAMESPACE_GUID), Mockito.eq(fileName), Mockito.any(),
                                         Mockito.eq(20L)))
               .thenReturn(fileEntry);
        Mockito.when(fileService.getFile(Mockito.eq(SPACE_GUID), Mockito.eq(fileEntry.getId())))
               .thenReturn(fileEntry);

        ResponseEntity<Void> startUploadResponse = testedClass.startUploadFromUrl(SPACE_GUID, NAMESPACE_GUID,
                                                                                  ImmutableFileUrl.of(FILE_URL));

        assertEquals(startUploadResponse.getStatusCode(), HttpStatus.ACCEPTED);

        String jobUrl = startUploadResponse.getHeaders()
                                           .getFirst("Location");
        String jobGuid = jobUrl.substring(jobUrl.lastIndexOf('/'));

        ResponseEntity<AsyncUploadResult> uploadJobResponse = testedClass.getUploadFromUrlJob(SPACE_GUID, NAMESPACE_GUID, jobGuid);

        assertEquals(uploadJobResponse.getStatusCode(), HttpStatus.CREATED);

        Mockito.verify(fileService)
               .addFile(Mockito.eq(SPACE_GUID), Mockito.eq(NAMESPACE_GUID), Mockito.eq(fileName), Mockito.any(), Mockito.eq(20L));

        var responseBody = uploadJobResponse.getBody();
        var fileMetadata = responseBody.getFile();
        assertMetadataMatches(fileEntry, fileMetadata);

        var mtaId = responseBody.getMtaId();
        var status = responseBody.getStatus();
        assertEquals(mtaId, MTA_ID);
        assertEquals(status, AsyncUploadResult.JobStatus.FINISHED);
    }

    @Test
    void testUploadFileFromUrlWithInvalidJobId() {
        AsyncUploadJobsQuery query = Mockito.mock(AsyncUploadJobsQuery.class, Answers.RETURNS_SELF);
        Mockito.doThrow(NoResultException.class)
               .when(query)
               .singleResult();

        Mockito.when(uploadJobService.createQuery())
               .thenReturn(query);

        ResponseEntity<AsyncUploadResult> response = testedClass.getUploadFromUrlJob(SPACE_GUID, NAMESPACE_GUID, "invalid");

        assertEquals(response.getStatusCode(), HttpStatus.NOT_FOUND);
    }

    @Test
    void testFileUrlDoesntReturnContentLength() throws Exception {
        HttpHeaders headers = HttpHeaders.of(Collections.emptyMap(), (a, b) -> true);
        AsyncUploadJobsQuery query = Mockito.mock(AsyncUploadJobsQuery.class, Answers.RETURNS_SELF);
        String error = "no content length";
        var jobEntry = mockUploadJobEntry(null, AsyncUploadJobEntry.State.ERROR, error);

        Mockito.when(fileUrlResponse.headers())
               .thenReturn(headers);
        Mockito.when(fileUrlResponse.statusCode())
               .thenReturn(200);

        Mockito.when(httpClient.send(Mockito.any(), Mockito.eq(BodyHandlers.ofInputStream())))
               .thenReturn(fileUrlResponse);

        Mockito.when(query.singleResult())
               .thenReturn(jobEntry);
        Mockito.when(uploadJobService.createQuery())
               .thenReturn(query);
        Mockito.when(uploadJobService.update(any(), any()))
               .thenReturn(jobEntry);

        ResponseEntity<Void> startUploadResponse = testedClass.startUploadFromUrl(SPACE_GUID, NAMESPACE_GUID,
                                                                                  ImmutableFileUrl.of(FILE_URL));

        assertEquals(startUploadResponse.getStatusCode(), HttpStatus.ACCEPTED);

        String jobUrl = startUploadResponse.getHeaders()
                                           .getFirst("Location");
        String jobGuid = jobUrl.substring(jobUrl.lastIndexOf('/'));

        ResponseEntity<AsyncUploadResult> uploadJobResponse = testedClass.getUploadFromUrlJob(SPACE_GUID, NAMESPACE_GUID, jobGuid);

        assertEquals(uploadJobResponse.getStatusCode(), HttpStatus.OK);

        var responseBody = uploadJobResponse.getBody();
        assertEquals(responseBody.getStatus(), AsyncUploadResult.JobStatus.ERROR);
        assertEquals(responseBody.getError(), error);
    }

    @Test
    void testFileUrlReturnsContentLengthAboveMaxUploadSize() throws Exception {
        long invalidFileSize = MAX_PERMITTED_SIZE + 1024;
        String fileSize = Long.toString(invalidFileSize);
        AsyncUploadJobsQuery query = Mockito.mock(AsyncUploadJobsQuery.class, Answers.RETURNS_SELF);
        String error = "content length exceeds max permitted size of 4GB";
        HttpHeaders headers = HttpHeaders.of(Map.of("Content-Length", List.of(fileSize)), (a, b) -> true);
        var jobEntry = mockUploadJobEntry(null, AsyncUploadJobEntry.State.ERROR, error);

        Mockito.when(fileUrlResponse.headers())
               .thenReturn(headers);
        Mockito.when(fileUrlResponse.statusCode())
               .thenReturn(200);

        Mockito.when(httpClient.send(Mockito.any(), Mockito.eq(BodyHandlers.ofInputStream())))
               .thenReturn(fileUrlResponse);

        Mockito.when(query.singleResult())
               .thenReturn(jobEntry);
        Mockito.when(uploadJobService.createQuery())
               .thenReturn(query);
        Mockito.when(uploadJobService.update(any(), any()))
               .thenReturn(jobEntry);

        ResponseEntity<Void> startUploadResponse = testedClass.startUploadFromUrl(SPACE_GUID, NAMESPACE_GUID,
                                                                                  ImmutableFileUrl.of(FILE_URL));

        assertEquals(startUploadResponse.getStatusCode(), HttpStatus.ACCEPTED);

        String jobUrl = startUploadResponse.getHeaders()
                                           .getFirst("Location");
        String jobGuid = jobUrl.substring(jobUrl.lastIndexOf('/'));

        ResponseEntity<AsyncUploadResult> uploadJobResponse = testedClass.getUploadFromUrlJob(SPACE_GUID, NAMESPACE_GUID, jobGuid);

        assertEquals(uploadJobResponse.getStatusCode(), HttpStatus.OK);

        var responseBody = uploadJobResponse.getBody();
        assertEquals(responseBody.getStatus(), AsyncUploadResult.JobStatus.ERROR);
        assertEquals(responseBody.getError(), error);
    }

    @ParameterizedTest
    @ValueSource(strings = { "https://host.domain/path/file?query=true", "http://host.domain/path/file.mtar?query=true" })
    void testUploadFileWithInvalidUrl(String url) throws Exception {
        AsyncUploadJobsQuery query = Mockito.mock(AsyncUploadJobsQuery.class, Answers.RETURNS_SELF);
        HttpHeaders headers = HttpHeaders.of(Map.of("Content-Length", List.of("20")), (a, b) -> true);
        var jobEntry = mockUploadJobEntry(null, AsyncUploadJobEntry.State.ERROR, "error");

        Mockito.when(fileUrlResponse.statusCode())
               .thenReturn(200);
        Mockito.when(fileUrlResponse.headers())
               .thenReturn(headers);

        Mockito.when(httpClient.send(Mockito.any(), Mockito.eq(BodyHandlers.ofInputStream())))
               .thenReturn(fileUrlResponse);

        Mockito.when(query.singleResult())
               .thenReturn(jobEntry);
        Mockito.when(uploadJobService.createQuery())
               .thenReturn(query);
        Mockito.when(uploadJobService.update(any(), any()))
               .thenReturn(jobEntry);

        String invalidFileUrl = Base64.getUrlEncoder()
                                      .encodeToString(url.getBytes(StandardCharsets.UTF_8));

        ResponseEntity<Void> startUploadResponse = testedClass.startUploadFromUrl(SPACE_GUID, NAMESPACE_GUID,
                                                                                  ImmutableFileUrl.of(invalidFileUrl));

        assertEquals(startUploadResponse.getStatusCode(), HttpStatus.ACCEPTED);

        String jobUrl = startUploadResponse.getHeaders()
                                           .getFirst("Location");
        String jobGuid = jobUrl.substring(jobUrl.lastIndexOf('/'));

        ResponseEntity<AsyncUploadResult> uploadJobResponse = testedClass.getUploadFromUrlJob(SPACE_GUID, NAMESPACE_GUID, jobGuid);

        assertEquals(uploadJobResponse.getStatusCode(), HttpStatus.OK);

        var responseBody = uploadJobResponse.getBody();
        assertEquals(responseBody.getStatus(), AsyncUploadResult.JobStatus.ERROR);
    }

    private void assertMetadataMatches(FileEntry expected, FileMetadata actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getSpace(), actual.getSpace());
        assertEquals(expected.getSize(), actual.getSize());
        assertEquals(expected.getDigest(), actual.getDigest());
        assertEquals(expected.getDigestAlgorithm(), actual.getDigestAlgorithm());
    }

    private FileEntry createFileEntry(String name) {
        return ImmutableFileEntry.builder()
                                 .id(UUID.randomUUID()
                                         .toString())
                                 .digest(RandomStringUtils.random(32, DIGEST_CHARACTER_TABLE))
                                 .digestAlgorithm(Constants.DIGEST_ALGORITHM)
                                 .name(name)
                                 .namespace(NAMESPACE_GUID)
                                 .size(BigInteger.valueOf(new Random().nextInt(1024 * 1024 * 10)))
                                 .space(SPACE_GUID)
                                 .build();
    }

    private AsyncUploadJobEntry mockUploadJobEntry(String fileId, AsyncUploadJobEntry.State jobState, String error) {
        AsyncUploadJobEntry jobEntry = Mockito.mock(AsyncUploadJobEntry.class);
        when(jobEntry.getId()).thenReturn(MTA_ID);
        when(jobEntry.getMtaId()).thenReturn(MTA_ID);
        when(jobEntry.getUser()).thenReturn("user1");
        when(jobEntry.getSpaceGuid()).thenReturn(SPACE_GUID);
        when(jobEntry.getStartedAt()).thenReturn(LocalDateTime.MIN);
        when(jobEntry.getFileId()).thenReturn(fileId);
        when(jobEntry.getState()).thenReturn(jobState);
        when(jobEntry.getUrl()).thenReturn("https://artifactory.sap/mta");
        if (jobState == AsyncUploadJobEntry.State.ERROR) {
            when(jobEntry.getError()).thenReturn(error);
        }
        return jobEntry;
    }
}
