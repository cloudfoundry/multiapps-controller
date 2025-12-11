package org.cloudfoundry.multiapps.controller.web.upload.client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.MessageFormat;
import java.util.OptionalLong;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.api.model.UserCredentials;
import org.cloudfoundry.multiapps.controller.client.util.CheckedSupplier;
import org.cloudfoundry.multiapps.controller.client.util.ResilientOperationExecutor;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.AsyncUploadJobEntry;
import org.cloudfoundry.multiapps.controller.web.Constants;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.cloudfoundry.multiapps.controller.web.upload.UploadFromUrlContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeployFromUrlRemoteClientTest {

    private static final String SECURE_URL = "https://example.com/file.zip";
    private static final String INSECURE_URL = "http://example.com/file.zip";
    private static final String URL_WITH_CREDENTIALS = "https://user:pass@example.com/file.zip";
    private static final String URL_WITH_ENCODED_CHARS = "https://example.com/file%20with%20spaces.zip";
    private static final long FILE_SIZE = 1024L;
    private static final long MAX_UPLOAD_SIZE = 2048L;

    @Mock
    private ApplicationConfiguration applicationConfiguration;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<InputStream> httpResponse;

    @Mock
    private HttpHeaders httpHeaders;

    @Mock
    private UserCredentials userCredentials;

    @Mock
    private UploadFromUrlContext uploadContext;

    @Mock
    private AsyncUploadJobEntry jobEntry;

    private TestableDeployFromUrlRemoteClient client;

    private class TestableDeployFromUrlRemoteClient extends DeployFromUrlRemoteClient {

        public TestableDeployFromUrlRemoteClient(ApplicationConfiguration applicationConfiguration) {
            super(applicationConfiguration);
        }

        @Override
        protected HttpClient buildHttpClient() {
            return httpClient;
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

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        client = new TestableDeployFromUrlRemoteClient(applicationConfiguration);
        when(applicationConfiguration.getMaxUploadSize()).thenReturn(MAX_UPLOAD_SIZE);
        when(uploadContext.getFileUrl()).thenReturn(SECURE_URL);
        when(uploadContext.getUserCredentials()).thenReturn(userCredentials);
        when(uploadContext.getJobEntry()).thenReturn(jobEntry);
        when(jobEntry.getId()).thenReturn("test-job-id");
        when(userCredentials.getUsername()).thenReturn("testUser");
        when(userCredentials.getPassword()).thenReturn("testPassword");
    }

    @Test
    void downloadFileFromUrlSuccess() throws Exception {
        InputStream inputStream = new ByteArrayInputStream("test content".getBytes());
        when(httpResponse.body()).thenReturn(inputStream);
        when(httpResponse.headers()).thenReturn(httpHeaders);
        when(httpResponse.uri()).thenReturn(URI.create(SECURE_URL));
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpHeaders.firstValueAsLong(Constants.CONTENT_LENGTH)).thenReturn(OptionalLong.of(FILE_SIZE));
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofInputStream()))).thenReturn(httpResponse);

        FileFromUrlData result = client.downloadFileFromUrl(uploadContext);

        assertEquals(inputStream, result.fileInputStream());
        assertEquals(URI.create(SECURE_URL), result.uri());
        assertEquals(FILE_SIZE, result.fileSize());
    }

    @Test
    void downloadFileFromUrlInsecureUrl() {
        when(uploadContext.getFileUrl()).thenReturn(INSECURE_URL);
        Exception exception = assertThrows(SLException.class,
                                           () -> client.downloadFileFromUrl(uploadContext));
        assertEquals(MessageFormat.format(Messages.MTAR_ENDPOINT_NOT_SECURE_FOR_JOB_WITH_ID, "test-job-id"), exception.getMessage());
    }

    @Test
    void downloadFileFromUrlNoContentLength() throws Exception {
        when(httpResponse.headers()).thenReturn(httpHeaders);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpHeaders.firstValueAsLong(Constants.CONTENT_LENGTH)).thenReturn(OptionalLong.empty());
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofInputStream()))).thenReturn(httpResponse);

        SLException exception = assertThrows(SLException.class,
                                             () -> client.downloadFileFromUrl(uploadContext));
        assertEquals(MessageFormat.format(Messages.FILE_URL_RESPONSE_DID_NOT_RETURN_CONTENT_LENGTH_FOR_JOB_WITH_ID, "test-job-id"), exception.getMessage());

    }

    @Test
    void downloadFileFromUrlFileSizeExceedsLimit() throws Exception {
        long oversizedFile = MAX_UPLOAD_SIZE + 1;
        when(httpResponse.headers()).thenReturn(httpHeaders);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpHeaders.firstValueAsLong(Constants.CONTENT_LENGTH)).thenReturn(OptionalLong.of(oversizedFile));
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofInputStream()))).thenReturn(httpResponse);

        SLException exception = assertThrows(SLException.class,
                                             () -> client.downloadFileFromUrl(uploadContext));
        assertEquals(MessageFormat.format(Messages.MAX_UPLOAD_SIZE_EXCEEDED_FOR_JOB_WITH_ID, MAX_UPLOAD_SIZE, "test-job-id"),
                     exception.getMessage());

    }

    @Test
    void callRemoteEndpointWithRetrySuccess() throws Exception {
        InputStream inputStream = new ByteArrayInputStream("test content".getBytes());
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(inputStream);
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofInputStream()))).thenReturn(httpResponse);

        when(httpResponse.headers()).thenReturn(httpHeaders);
        when(httpHeaders.firstValueAsLong(Constants.CONTENT_LENGTH)).thenReturn(OptionalLong.of(FILE_SIZE));

        FileFromUrlData result = client.downloadFileFromUrl(uploadContext);
        verify(httpClient).send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofInputStream()));
        assertEquals(1024, result.fileSize());
    }

    @Test
    void callRemoteEndpointWithRetryUnauthorizedError() throws Exception {
        InputStream errorStream = new ByteArrayInputStream("Unauthorized".getBytes());
        when(httpResponse.statusCode()).thenReturn(401);
        when(httpResponse.body()).thenReturn(errorStream);
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofInputStream()))).thenReturn(httpResponse);

        SLException exception = assertThrows(SLException.class,
                                             () -> client.downloadFileFromUrl(uploadContext));
        assertEquals(MessageFormat.format(Messages.DEPLOY_FROM_URL_WRONG_CREDENTIALS_FOR_JOB_WITH_ID, "https://example.com/file.zip", "test-job-id"), exception.getMessage());
    }

    @Test
    void callRemoteEndpointWithRetryServerError() throws Exception {
        InputStream errorStream = new ByteArrayInputStream("Internal Server Error".getBytes());
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpResponse.body()).thenReturn(errorStream);
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofInputStream()))).thenReturn(httpResponse);

        SLException exception = assertThrows(SLException.class,
                                             () -> client.downloadFileFromUrl(uploadContext));
        assertTrue(exception.getMessage()
                            .contains("500"));

    }

    @Test
    void downloadFileFromUrlWithNullUserCredentials() throws Exception {
        when(uploadContext.getUserCredentials()).thenReturn(null);
        InputStream inputStream = new ByteArrayInputStream("test content".getBytes());
        when(httpResponse.body()).thenReturn(inputStream);
        when(httpResponse.headers()).thenReturn(httpHeaders);
        when(httpResponse.uri()).thenReturn(URI.create(SECURE_URL));
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpHeaders.firstValueAsLong(Constants.CONTENT_LENGTH)).thenReturn(OptionalLong.of(FILE_SIZE));
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofInputStream()))).thenReturn(httpResponse);

        FileFromUrlData result = client.downloadFileFromUrl(uploadContext);

        assertEquals(inputStream, result.fileInputStream());
        assertEquals(URI.create(SECURE_URL), result.uri());
        assertEquals(FILE_SIZE, result.fileSize());

    }

    @Test
    void resilientOperationExecutorExceptionHandling() throws Exception {
        Exception testException = new RuntimeException("Test exception");
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofInputStream()))).thenThrow(testException);

        RuntimeException exception = assertThrows(RuntimeException.class,
                                                  () -> client.downloadFileFromUrl(uploadContext));
        assertEquals("Test exception", exception.getMessage());

    }

    @Test
    void downloadFileFromUrlWithCredentialsInUrl() throws Exception {
        when(uploadContext.getFileUrl()).thenReturn(URL_WITH_CREDENTIALS);
        when(uploadContext.getUserCredentials()).thenReturn(null);
        InputStream inputStream = new ByteArrayInputStream("test content".getBytes());
        when(httpResponse.body()).thenReturn(inputStream);
        when(httpResponse.headers()).thenReturn(httpHeaders);
        when(httpResponse.uri()).thenReturn(URI.create("https://example.com/file.zip"));
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpHeaders.firstValueAsLong(Constants.CONTENT_LENGTH)).thenReturn(OptionalLong.of(FILE_SIZE));
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofInputStream()))).thenReturn(httpResponse);

        FileFromUrlData result = client.downloadFileFromUrl(uploadContext);

        assertEquals(inputStream, result.fileInputStream());
        assertEquals(URI.create("https://example.com/file.zip"), result.uri());
        assertEquals(FILE_SIZE, result.fileSize());

        verify(httpClient).send(argThat(request ->
                                            !request.uri()
                                                    .toString()
                                                    .contains("user:pass@") &&
                                                request.headers()
                                                       .firstValue("Authorization")
                                                       .isPresent() &&
                                                request.headers()
                                                       .firstValue("Authorization")
                                                       .get()
                                                       .startsWith("Basic ")
        ), eq(HttpResponse.BodyHandlers.ofInputStream()));

    }

    @Test
    void downloadFileFromUrlWithEncodedCharacters() throws Exception {
        when(uploadContext.getFileUrl()).thenReturn(URL_WITH_ENCODED_CHARS);
        InputStream inputStream = new ByteArrayInputStream("test content".getBytes());
        when(httpResponse.body()).thenReturn(inputStream);
        when(httpResponse.headers()).thenReturn(httpHeaders);
        when(httpResponse.uri()).thenReturn(URI.create("https://example.com/file%20with%20spaces.zip"));
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpHeaders.firstValueAsLong(Constants.CONTENT_LENGTH)).thenReturn(OptionalLong.of(FILE_SIZE));
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofInputStream()))).thenReturn(httpResponse);

        FileFromUrlData result = client.downloadFileFromUrl(uploadContext);

        assertEquals(inputStream, result.fileInputStream());
        assertEquals(URI.create("https://example.com/file%20with%20spaces.zip"), result.uri());
        assertEquals(FILE_SIZE, result.fileSize());

    }

}
