package org.cloudfoundry.multiapps.controller.web.api.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadBase.SizeLimitExceededException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang3.RandomStringUtils;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.api.model.FileMetadata;
import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingFacade;
import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingProvider;
import org.cloudfoundry.multiapps.controller.persistence.Constants;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableFileEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.persistence.util.Configuration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class FilesApiServiceImplTest {

    @Mock
    private FileService fileService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private FileItemIterator fileItemIterator;
    @Mock
    private FileItemStream fileItemStream;
    @Mock
    private ServletFileUpload servletFileUpload;
    @Mock
    private HttpClient httpClient;
    @Mock
    private HttpResponse<InputStream> fileUrlResponse;

    private static final long MAX_PERMITTED_SIZE = new Configuration().getMaxUploadSize();
    private static final String FILE_URL = Base64.getUrlEncoder()
                                                 .encodeToString("http://host.domain/test.mtar?query=true".getBytes(StandardCharsets.UTF_8));

    @InjectMocks
    private final FilesApiServiceImpl testedClass = new FilesApiServiceImpl() {
        @Override
        protected ServletFileUpload getFileUploadServlet() {
            return servletFileUpload;
        }

        @Override
        protected HttpClient buildHttpClient(String url) {
            return httpClient;
        }
    };

    private static final String SPACE_GUID = "896e6be9-8217-4a1c-b938-09b30966157a";
    private static final String NAMESPACE_GUID = "0a42c085-b772-4b1e-bf4d-75c463aab5f6";

    private static final String DIGEST_CHARACTER_TABLE = "123456789ABCDEF";

    @BeforeEach
    public void initialize() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        Mockito.when(request.getRequestURI())
               .thenReturn("");
        AuditLoggingProvider.setFacade(Mockito.mock(AuditLoggingFacade.class));
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

        Mockito.when(servletFileUpload.getItemIterator(Mockito.eq(request)))
               .thenReturn(fileItemIterator);
        Mockito.when(fileItemIterator.hasNext())
               .thenReturn(true, true, false); // has two form entries
        Mockito.when(fileItemIterator.next())
               .thenReturn(fileItemStream);
        Mockito.when(fileItemStream.isFormField())
               .thenReturn(false, true); // only first entry is a file
        Mockito.when(fileItemStream.openStream())
               .thenReturn(Mockito.mock(InputStream.class));
        Mockito.when(fileItemStream.getName())
               .thenReturn(fileName);
        Mockito.when(fileService.addFile(Mockito.eq(SPACE_GUID), Mockito.eq(NAMESPACE_GUID), Mockito.eq(fileName),
                                         Mockito.any(InputStream.class)))
               .thenReturn(fileEntry);

        ResponseEntity<FileMetadata> response = testedClass.uploadFile(request, SPACE_GUID, NAMESPACE_GUID, null);

        Mockito.verify(servletFileUpload)
               .setSizeMax(Mockito.eq(new Configuration().getMaxUploadSize()));
        Mockito.verify(fileItemIterator, Mockito.times(3))
               .hasNext();
        Mockito.verify(fileItemStream)
               .openStream();
        Mockito.verify(fileService)
               .addFile(Mockito.eq(SPACE_GUID), Mockito.eq(NAMESPACE_GUID), Mockito.eq(fileName), Mockito.any(InputStream.class));

        FileMetadata fileMetadata = response.getBody();
        assertMetadataMatches(fileEntry, fileMetadata);
    }

    @Test
    void testUploadMtaFileErrorSizeExceeded() throws Exception {
        Mockito.when(servletFileUpload.getItemIterator(Mockito.eq(request)))
               .thenThrow(new SizeLimitExceededException("size limit exceeded", MAX_PERMITTED_SIZE + 1024, MAX_PERMITTED_SIZE));
        Assertions.assertThrows(SLException.class, () -> testedClass.uploadFile(request, SPACE_GUID, null, null));
    }

    @Test
    void testUploadFileFromUrl() throws Exception {
        HttpHeaders headers = HttpHeaders.of(Map.of("Content-Length", List.of("20")), (a, b) -> true);
        Mockito.when(fileUrlResponse.headers())
               .thenReturn(headers);
        Mockito.when(fileUrlResponse.statusCode())
               .thenReturn(200);
        Mockito.when(fileUrlResponse.body())
               .thenReturn(InputStream.nullInputStream());

        Mockito.when(httpClient.send(Mockito.any(), Mockito.eq(BodyHandlers.ofInputStream())))
               .thenReturn(fileUrlResponse);

        String fileName = "test.mtar";
        FileEntry fileEntry = createFileEntry(fileName);

        Mockito.when(fileService.addFile(Mockito.eq(SPACE_GUID), Mockito.eq(NAMESPACE_GUID), Mockito.eq(fileName),
                                         Mockito.any(InputStream.class), Mockito.eq(20L)))
               .thenReturn(fileEntry);

        ResponseEntity<FileMetadata> response = testedClass.uploadFile(request, SPACE_GUID, NAMESPACE_GUID, FILE_URL);

        Mockito.verify(fileService)
               .addFile(Mockito.eq(SPACE_GUID), Mockito.eq(NAMESPACE_GUID), Mockito.eq(fileName), Mockito.any(InputStream.class),
                        Mockito.eq(20L));

        FileMetadata fileMetadata = response.getBody();
        assertMetadataMatches(fileEntry, fileMetadata);
    }

    @Test
    void testFileUrlDoesntReturnContentLength() throws Exception {
        HttpHeaders headers = HttpHeaders.of(Collections.emptyMap(), (a, b) -> true);
        Mockito.when(fileUrlResponse.headers())
               .thenReturn(headers);
        Mockito.when(fileUrlResponse.statusCode())
               .thenReturn(200);

        Mockito.when(httpClient.send(Mockito.any(), Mockito.eq(BodyHandlers.ofInputStream())))
               .thenReturn(fileUrlResponse);

        Assertions.assertThrows(SLException.class, () -> testedClass.uploadFile(request, SPACE_GUID, NAMESPACE_GUID, FILE_URL));
    }

    @Test
    void testFileUrlReturnsContentLengthAboveMaxUploadSize() throws Exception {
        long invalidFileSize = MAX_PERMITTED_SIZE + 1024;
        String fileSize = Long.toString(invalidFileSize);
        HttpHeaders headers = HttpHeaders.of(Map.of("Content-Length", List.of(fileSize)), (a, b) -> true);
        Mockito.when(fileUrlResponse.headers())
               .thenReturn(headers);
        Mockito.when(fileUrlResponse.statusCode())
               .thenReturn(200);

        Mockito.when(httpClient.send(Mockito.any(), Mockito.eq(BodyHandlers.ofInputStream())))
               .thenReturn(fileUrlResponse);

        Assertions.assertThrows(SLException.class, () -> testedClass.uploadFile(request, SPACE_GUID, NAMESPACE_GUID, FILE_URL));
    }

    @Test
    void testUploadFileWithInvalidName() throws Exception {
        HttpHeaders headers = HttpHeaders.of(Map.of("Content-Length", List.of("20")), (a, b) -> true);
        Mockito.when(fileUrlResponse.statusCode())
               .thenReturn(200);
        Mockito.when(fileUrlResponse.headers())
               .thenReturn(headers);

        Mockito.when(httpClient.send(Mockito.any(), Mockito.eq(BodyHandlers.ofInputStream())))
               .thenReturn(fileUrlResponse);

        String fileUrlWithInvalidFileName = Base64.getUrlEncoder()
                                                  .encodeToString("http://host.domain/path/file?query=true".getBytes(StandardCharsets.UTF_8));
        Assertions.assertThrows(SLException.class,
                                () -> testedClass.uploadFile(request, SPACE_GUID, NAMESPACE_GUID, fileUrlWithInvalidFileName));
    }

    @Test
    void testUploadFileServerError() throws Exception {
        Mockito.when(fileUrlResponse.statusCode())
               .thenReturn(500);
        String body = "Internal Server Error";
        Mockito.when(fileUrlResponse.body())
               .thenReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));

        Mockito.when(httpClient.send(Mockito.any(), Mockito.eq(BodyHandlers.ofInputStream())))
               .thenReturn(fileUrlResponse);

        Assertions.assertThrows(SLException.class, () -> testedClass.uploadFile(request, SPACE_GUID, NAMESPACE_GUID, FILE_URL));
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
}
