package org.cloudfoundry.multiapps.controller.web.upload.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.commons.io.IOUtils;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.api.model.UserCredentials;
import org.cloudfoundry.multiapps.controller.client.util.CheckedSupplier;
import org.cloudfoundry.multiapps.controller.client.util.ResilientOperationExecutor;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.UriUtil;
import org.cloudfoundry.multiapps.controller.web.Constants;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.cloudfoundry.multiapps.controller.web.upload.UploadFromUrlContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

@Named
public class DeployFromUrlRemoteClient {

    private static final Duration HTTP_CONNECT_TIMEOUT = Duration.ofMinutes(10);
    private static final String USERNAME_PASSWORD_URL_FORMAT = "{0}:{1}";
    private static final int ERROR_RESPONSE_BODY_MAX_LENGTH = 4 * 1024;

    private static final Logger LOGGER = LoggerFactory.getLogger(DeployFromUrlRemoteClient.class);

    private final HttpClient httpClient = buildHttpClient();
    private final ResilientOperationExecutor resilientOperationExecutor = getResilientOperationExecutor();

    private final ApplicationConfiguration applicationConfiguration;

    @Inject
    public DeployFromUrlRemoteClient(ApplicationConfiguration applicationConfiguration) {
        this.applicationConfiguration = applicationConfiguration;
    }

    public FileFromUrlData downloadFileFromUrl(UploadFromUrlContext uploadFromUrlContext) throws Exception {
        if (!UriUtil.isUrlSecure(uploadFromUrlContext.getFileUrl())) {
            throw new SLException(Messages.MTAR_ENDPOINT_NOT_SECURE_FOR_JOB_WITH_ID, uploadFromUrlContext.getJobEntry()
                                                                                                         .getId());
        }
        UriUtil.validateUrl(uploadFromUrlContext.getFileUrl());

        HttpResponse<InputStream> response = callRemoteEndpointWithRetry(uploadFromUrlContext.getFileUrl(),
                                                                         uploadFromUrlContext.getJobEntry()
                                                                                             .getId(),
                                                                         uploadFromUrlContext.getUserCredentials());
        long fileSize = response.headers()
                                .firstValueAsLong(Constants.CONTENT_LENGTH)
                                .orElseThrow(() -> new SLException(
                                    MessageFormat.format(Messages.FILE_URL_RESPONSE_DID_NOT_RETURN_CONTENT_LENGTH_FOR_JOB_WITH_ID,
                                                         uploadFromUrlContext.getJobEntry()
                                                                             .getId())));

        long maxUploadSize = applicationConfiguration.getMaxUploadSize();
        if (fileSize > maxUploadSize) {
            throw new SLException(MessageFormat.format(Messages.MAX_UPLOAD_SIZE_EXCEEDED_FOR_JOB_WITH_ID, maxUploadSize,
                                                       uploadFromUrlContext.getJobEntry()
                                                                           .getId()));
        }
        return new FileFromUrlData(response.body(), response.uri(), fileSize);
    }

    private HttpResponse<InputStream> callRemoteEndpointWithRetry(String decodedUrl, String jobId, UserCredentials userCredentials)
        throws Exception {
        return resilientOperationExecutor.execute((CheckedSupplier<HttpResponse<InputStream>>) () -> {
            var request = buildFetchFileRequest(decodedUrl, userCredentials);
            LOGGER.debug(Messages.CALLING_REMOTE_MTAR_ENDPOINT_FOR_JOB_WITH_ID, getMaskedUri(urlDecodeUrl(decodedUrl)), jobId);
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() / 100 != 2) {
                String error = readErrorBodyFromResponse(response);
                LOGGER.error(error);
                if (response.statusCode() == HttpStatus.UNAUTHORIZED.value()) {
                    String errorMessage = MessageFormat.format(Messages.DEPLOY_FROM_URL_WRONG_CREDENTIALS_FOR_JOB_WITH_ID,
                                                               UriUtil.stripUserInfo(decodedUrl), jobId);
                    throw new SLException(errorMessage);
                }
                throw new SLException(
                    MessageFormat.format(Messages.ERROR_FROM_REMOTE_MTAR_ENDPOINT_FOR_JOB_WITH_ID, getMaskedUri(urlDecodeUrl(decodedUrl)),
                                         response.statusCode(), error, jobId));
            }
            return response;
        });
    }

    private String getMaskedUri(String url) {
        if (url.contains("@")) {
            return url.substring(url.lastIndexOf("@"))
                      .replace("@", "...");
        } else {
            return url;
        }
    }

    private String urlDecodeUrl(String url) {
        return URLDecoder.decode(url, StandardCharsets.UTF_8);
    }

    private HttpRequest buildFetchFileRequest(String decodedUrl, UserCredentials userCredentials) {
        var builder = HttpRequest.newBuilder()
                                 .GET()
                                 .timeout(Duration.ofMinutes(15));
        var uri = URI.create(decodedUrl);
        var userInfo = uri.getUserInfo();
        if (userCredentials != null) {
            builder.uri(uri);
            String userCredentialsUrlFormat = MessageFormat.format(USERNAME_PASSWORD_URL_FORMAT, userCredentials.getUsername(),
                                                                   userCredentials.getPassword());
            String encodedAuth = Base64.getEncoder()
                                       .encodeToString(userCredentialsUrlFormat.getBytes());
            builder.header(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);
        } else if (userInfo != null) {
            builder.uri(URI.create(decodedUrl.replace(uri.getRawUserInfo() + "@", "")));
            String encodedAuth = Base64.getEncoder()
                                       .encodeToString(userInfo.getBytes());
            builder.header(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);
        } else {
            builder.uri(uri);
        }
        return builder.build();
    }

    private String readErrorBodyFromResponse(HttpResponse<InputStream> response) throws IOException {
        try (InputStream is = response.body()) {
            byte[] buffer = new byte[ERROR_RESPONSE_BODY_MAX_LENGTH];
            int read = IOUtils.read(is, buffer);
            return new String(Arrays.copyOf(buffer, read));
        }
    }

    protected HttpClient buildHttpClient() {
        return HttpClient.newBuilder()
                         .version(HttpClient.Version.HTTP_2)
                         .connectTimeout(HTTP_CONNECT_TIMEOUT)
                         .followRedirects(HttpClient.Redirect.NORMAL)
                         .build();
    }

    protected ResilientOperationExecutor getResilientOperationExecutor() {
        return new ResilientOperationExecutor();
    }
}
