package org.cloudfoundry.multiapps.controller.shutdown.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.core.http.CsrfHttpClient;
import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdown;

class ShutdownClientImpl implements ShutdownClient {

    private static final String SHUTDOWN_ENDPOINT = "/rest/admin/shutdown";
    private static final String TRIGGER_SHUTDOWN_ENDPOINT = "https://{0}{1}?applicationId={2}&instancesCount={3}";
    private static final String SHUTDOWN_STATUS_ENDPOINT = "https://{0}{1}?applicationId={2}";

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
    public List<ApplicationShutdown> triggerShutdown(UUID applicationGuid, int applicationInstancesCount) {
        String a = getTriggerShutdownEndpoint(applicationGuid, applicationInstancesCount);

        HttpPost request = new HttpPost(a);
        return makeShutdownApiRequest(request);
    }

    @Override
    public List<ApplicationShutdown> getStatus(UUID applicationGuid) {
        HttpGet request = new HttpGet(getShutdownStatusEndpoint(applicationGuid));
        return makeShutdownApiRequest(request);
    }

    private String getShutdownStatusEndpoint(UUID applicationGuid) {
        return MessageFormat.format(SHUTDOWN_STATUS_ENDPOINT, applicationUrl, SHUTDOWN_ENDPOINT, applicationGuid);
    }

    private String getTriggerShutdownEndpoint(UUID applicationGuid, int applicationInstancesCount) {
        return MessageFormat.format(TRIGGER_SHUTDOWN_ENDPOINT, applicationUrl, SHUTDOWN_ENDPOINT, applicationGuid,
                                    applicationInstancesCount);
    }

    private List<ApplicationShutdown> makeShutdownApiRequest(HttpUriRequest httpRequest) {
        try (CsrfHttpClient csrfHttpClient = createCsrfHttpClient()) {
            return csrfHttpClient.execute(httpRequest, ShutdownClientImpl::parse);
        } catch (IOException e) {
            throw new IllegalStateException(MessageFormat.format("Could not parse shutdown API response: {0}", e.getMessage()), e);
        }
    }

    private CsrfHttpClient createCsrfHttpClient() {
        return httpClientFactory.create(Map.of());
    }

    private static List<ApplicationShutdown> parse(ClassicHttpResponse response) throws IOException, ParseException {
        String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        return parse(body);
    }

    private static List<ApplicationShutdown> parse(String body) {
        return JsonUtil.fromJson(body, new TypeReference<List<ApplicationShutdown>>() {
        });
    }

}
