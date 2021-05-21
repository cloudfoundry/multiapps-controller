package org.cloudfoundry.multiapps.controller.shutdown.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Map;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.core.http.CsrfHttpClient;
import org.cloudfoundry.multiapps.controller.core.model.ApplicationShutdown;

class ShutdownClientImpl implements ShutdownClient {

    private static final String X_CF_APP_INSTANCE = "x-cf-app-instance";
    private static final String SHUTDOWN_ENDPOINT = "/rest/admin/shutdown";

    private final String applicationUrl;
    /**
     * We need to create a new instance of {@link CsrfHttpClient} for each request, because the default headers of the client may differ
     * based on the application's GUID and instance index.
     */
    private final CsrfHttpClientFactory httpClientFactory;

    ShutdownClientImpl(String applicationUrl, CsrfHttpClientFactory httpClientFactory) {
        this.applicationUrl = applicationUrl;
        this.httpClientFactory = httpClientFactory;
    }

    @Override
    public ApplicationShutdown triggerShutdown(UUID applicationGuid, int applicationInstanceIndex) {
        HttpPost request = new HttpPost(getShutdownEndpoint());
        return makeShutdownApiRequest(applicationGuid, applicationInstanceIndex, request);
    }

    @Override
    public ApplicationShutdown getStatus(UUID applicationGuid, int applicationInstanceIndex) {
        HttpGet request = new HttpGet(getShutdownEndpoint());
        return makeShutdownApiRequest(applicationGuid, applicationInstanceIndex, request);
    }

    private String getShutdownEndpoint() {
        return applicationUrl + SHUTDOWN_ENDPOINT;
    }

    private ApplicationShutdown makeShutdownApiRequest(UUID applicationGuid, int applicationInstanceIndex, HttpUriRequest httpRequest) {
        try (CsrfHttpClient csrfHttpClient = createCsrfHttpClient(applicationGuid, applicationInstanceIndex)) {
            HttpResponse response = csrfHttpClient.execute(httpRequest);
            return parse(response);
        } catch (IOException e) {
            throw new IllegalStateException(MessageFormat.format("Could not parse shutdown API response: {0}", e.getMessage()), e);
        }
    }

    private CsrfHttpClient createCsrfHttpClient(UUID applicationGuid, int applicationInstanceIndex) {
        String applicationInstanceHeaderValue = computeApplicationInstanceHeaderValue(applicationGuid, applicationInstanceIndex);
        return httpClientFactory.create(Map.of(X_CF_APP_INSTANCE, applicationInstanceHeaderValue));
    }

    private static String computeApplicationInstanceHeaderValue(UUID applicationGuid, int applicationInstanceIndex) {
        return String.format("%s:%d", applicationGuid, applicationInstanceIndex);
    }

    private static ApplicationShutdown parse(HttpResponse response) throws IOException {
        String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        return parse(body);
    }

    private static ApplicationShutdown parse(String body) {
        return JsonUtil.fromJson(body, ApplicationShutdown.class);
    }

}
