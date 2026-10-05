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
import org.cloudfoundry.multiapps.controller.core.util.AddressValidator;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.LogSanitizer;
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
    private static final int MAX_REDIRECTS = 10;
    private static final String REDIRECT_WITH_NO_LOCATION_HEADER = "redirect with no Location header";

    private static final Logger LOGGER = LoggerFactory.getLogger(DeployFromUrlRemoteClient.class);

    private final HttpClient httpClient = buildHttpClient();
    private final ResilientOperationExecutor resilientOperationExecutor = getResilientOperationExecutor();

    private final ApplicationConfiguration applicationConfiguration;
    private final AddressValidator addressValidator;

    @Inject
    public DeployFromUrlRemoteClient(ApplicationConfiguration applicationConfiguration, AddressValidator addressValidator) {
        this.applicationConfiguration = applicationConfiguration;
        this.addressValidator = addressValidator;
    }

    public FileFromUrlData downloadFileFromUrl(UploadFromUrlContext uploadFromUrlContext) throws Exception {
        if (!UriUtil.isUrlSecure(uploadFromUrlContext.getFileUrl())) {
            throw new SLException(Messages.MTAR_ENDPOINT_NOT_SECURE_FOR_JOB_WITH_ID, uploadFromUrlContext.getJobEntry()
                                                                                                         .getId());
        }
        UriUtil.validateUrl(uploadFromUrlContext.getFileUrl());
        addressValidator.validateTarget(uploadFromUrlContext.getFileUrl(), uploadFromUrlContext.getJobEntry()
                                                                                               .getId());

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
            LOGGER.debug(Messages.CALLING_REMOTE_MTAR_ENDPOINT_FOR_JOB_WITH_ID, getMaskedUri(urlDecodeUrl(decodedUrl)), jobId);
            return followRedirectsWithValidation(decodedUrl, jobId, userCredentials, 0);
        });
    }

    private HttpResponse<InputStream> followRedirectsWithValidation(String url, String jobId, UserCredentials userCredentials,
                                                                    int redirectCount) throws Exception {
        if (redirectCount > MAX_REDIRECTS) {
            throw new SLException(MessageFormat.format(Messages.ERROR_FROM_REMOTE_MTAR_ENDPOINT_FOR_JOB_WITH_ID,
                                                       getMaskedUri(urlDecodeUrl(url)), Messages.TOO_MANY_REDIRECTS, "", jobId));
        }
        HttpRequest request = buildFetchFileRequest(url, userCredentials);
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        int status = response.statusCode();
        if (isRedirect(status)) {
            return followRedirect(response, url, jobId, userCredentials, redirectCount);
        }
        if (status >= 200 & status < 300) {
            throwErrorResponseException(response, url, status, jobId);
        }
        return response;
    }

    private HttpResponse<InputStream> followRedirect(HttpResponse<InputStream> response, String url, String jobId,
                                                     UserCredentials userCredentials, int redirectCount) throws Exception {
        int status = response.statusCode();
        String location = response.headers()
                                  .firstValue(HttpHeaders.LOCATION)
                                  .orElseThrow(() -> new SLException(
                                      MessageFormat.format(Messages.ERROR_FROM_REMOTE_MTAR_ENDPOINT_FOR_JOB_WITH_ID,
                                                           getMaskedUri(urlDecodeUrl(url)), status, REDIRECT_WITH_NO_LOCATION_HEADER,
                                                           jobId)));
        IOUtils.consume(response.body());
        String resolvedLocation = resolveLocation(url, location);
        if (!UriUtil.isUrlSecure(resolvedLocation)) {
            throw new SLException(Messages.MTAR_ENDPOINT_NOT_SECURE_FOR_JOB_WITH_ID, jobId);
        }
        addressValidator.validateTarget(resolvedLocation, jobId);
        return followRedirectsWithValidation(resolvedLocation, jobId, userCredentials, redirectCount + 1);
    }

    private void throwErrorResponseException(HttpResponse<InputStream> response, String url, int status,
                                             String jobId) throws IOException {
        String error = LogSanitizer.sanitize(readErrorBodyFromResponse(response));
        LOGGER.error(error);
        if (status == HttpStatus.UNAUTHORIZED.value()) {
            throw new SLException(MessageFormat.format(Messages.DEPLOY_FROM_URL_WRONG_CREDENTIALS_FOR_JOB_WITH_ID,
                                                       UriUtil.stripUserInfo(url), jobId));
        }
        throw new SLException(MessageFormat.format(Messages.ERROR_FROM_REMOTE_MTAR_ENDPOINT_FOR_JOB_WITH_ID,
                                                   getMaskedUri(urlDecodeUrl(url)), status, error, jobId));
    }

    private boolean isRedirect(int statusCode) {
        return statusCode == HttpStatus.MOVED_PERMANENTLY.value() || statusCode == HttpStatus.FOUND.value()
            || statusCode == HttpStatus.SEE_OTHER.value() || statusCode == HttpStatus.TEMPORARY_REDIRECT.value()
            || statusCode == HttpStatus.PERMANENT_REDIRECT.value();
    }

    private String resolveLocation(String base, String location) {
        URI locationUri = URI.create(location);
        if (locationUri.isAbsolute()) {
            return location;
        }
        return URI.create(base)
                  .resolve(locationUri)
                  .toString();
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
                         .followRedirects(HttpClient.Redirect.NEVER)
                         .build();
    }

    protected ResilientOperationExecutor getResilientOperationExecutor() {
        return new ResilientOperationExecutor();
    }
}
