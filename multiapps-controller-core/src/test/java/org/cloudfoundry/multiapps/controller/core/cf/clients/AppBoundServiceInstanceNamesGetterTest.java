package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.CloudCredentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppBoundServiceInstanceNamesGetterTest extends CustomControllerClientBaseTest {

    private static final String CORRELATION_ID = "test-correlation-id";

    private AppBoundServiceInstanceNamesGetter client;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        Mockito.when(applicationConfiguration.getVersion())
               .thenReturn("1.0.0");
        Mockito.when(webClientFactory.getWebClient(Mockito.any(CloudCredentials.class)))
               .thenReturn(webClient);

        client = new AppBoundServiceInstanceNamesGetter(applicationConfiguration, webClientFactory, new CloudCredentials("user", "pass"),
                                                        CORRELATION_ID);
    }

    @Test
    void testGetServiceInstanceNamesBoundToAppBuildsCorrectUri() {
        stubWebClientToReturnResponse();

        UUID appGuid = UUID.randomUUID();
        client.getServiceInstanceNamesBoundToApp(appGuid);

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(requestHeadersUriSpec)
               .uri(uriCaptor.capture());

        String capturedUri = uriCaptor.getValue();
        assertTrue(capturedUri.startsWith("/v3/service_credential_bindings?"),
                   "URI should start with the service credential bindings base path");
        assertTrue(capturedUri.contains("include=service_instance"),
                   "URI should include service_instance");
        assertTrue(capturedUri.contains("app_guids=" + appGuid),
                   "URI should contain the app guid");
    }

    @Test
    void testGetServiceInstanceNamesBoundToAppWithEmptyResponseReturnsEmptyList() {
        stubWebClientToReturnResponse();

        UUID appGuid = UUID.randomUUID();
        List<String> result = client.getServiceInstanceNamesBoundToApp(appGuid);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetServiceInstanceNamesBoundToAppReturnsSingleServiceName() {
        String serviceName = "my-database";
        String responseJson = buildResponseWithIncludedServiceInstances(List.of(serviceName), null);
        stubWebClientToReturnResponse(responseJson);

        UUID appGuid = UUID.randomUUID();
        List<String> result = client.getServiceInstanceNamesBoundToApp(appGuid);

        assertEquals(1, result.size());
        assertEquals(serviceName, result.getFirst());
    }

    @Test
    void testGetServiceInstanceNamesBoundToAppReturnsMultipleServiceNames() {
        List<String> serviceNames = List.of("db-service", "cache-service", "queue-service");
        String responseJson = buildResponseWithIncludedServiceInstances(serviceNames, null);
        stubWebClientToReturnResponse(responseJson);

        UUID appGuid = UUID.randomUUID();
        List<String> result = client.getServiceInstanceNamesBoundToApp(appGuid);

        assertEquals(3, result.size());
        assertEquals("db-service", result.get(0));
        assertEquals("cache-service", result.get(1));
        assertEquals("queue-service", result.get(2));
    }

    @Test
    void testGetServiceInstanceNamesBoundToAppDeduplicatesServiceNames() {
        List<String> serviceNames = List.of("my-service", "my-service", "other-service");
        String responseJson = buildResponseWithIncludedServiceInstances(serviceNames, null);
        stubWebClientToReturnResponse(responseJson);

        UUID appGuid = UUID.randomUUID();
        List<String> result = client.getServiceInstanceNamesBoundToApp(appGuid);

        assertEquals(2, result.size());
        assertEquals("my-service", result.get(0));
        assertEquals("other-service", result.get(1));
    }

    @Test
    void testGetServiceInstanceNamesBoundToAppWithNoIncludedResourcesReturnsEmptyList() {
        // Response has resources but no "included" section
        String responseJson = "{\"resources\":[{\"guid\":\"" + UUID.randomUUID() + "\"}],\"pagination\":{\"next\":null}}";
        stubWebClientToReturnResponse(responseJson);

        UUID appGuid = UUID.randomUUID();
        List<String> result = client.getServiceInstanceNamesBoundToApp(appGuid);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetServiceInstanceNamesBoundToAppPaginatedResponseFollowsAllPages() {
        String page1Json = buildResponseWithIncludedServiceInstances(
            List.of("svc-page1"), "/v3/service_credential_bindings?page=2");
        String page2Json = buildResponseWithIncludedServiceInstances(List.of("svc-page2"), null);

        stubWebClientToReturnResponse(page1Json, page2Json);

        UUID appGuid = UUID.randomUUID();
        List<String> result = client.getServiceInstanceNamesBoundToApp(appGuid);

        assertEquals(2, result.size());
        assertEquals("svc-page1", result.get(0));
        assertEquals("svc-page2", result.get(1));
    }

    @Test
    void testGetServiceInstanceNamesBoundToAppMakesExactlyOneHttpCallForSinglePage() {
        stubWebClientToReturnResponse();

        UUID appGuid = UUID.randomUUID();
        client.getServiceInstanceNamesBoundToApp(appGuid);

        Mockito.verify(webClient, Mockito.times(1))
               .get();
    }

    @Test
    void testGetServiceInstanceNamesBoundToAppMakesTwoHttpCallsForTwoPages() {
        String page1Json = buildResponseWithIncludedServiceInstances(
            List.of("svc-1"), "/v3/service_credential_bindings?page=2");
        String page2Json = buildResponseWithIncludedServiceInstances(List.of("svc-2"), null);

        stubWebClientToReturnResponse(page1Json, page2Json);

        UUID appGuid = UUID.randomUUID();
        client.getServiceInstanceNamesBoundToApp(appGuid);

        Mockito.verify(webClient, Mockito.times(2))
               .get();
    }

    @Test
    void testGetServiceInstanceNamesBoundToAppWithResourcesButEmptyIncludedServiceInstancesReturnsEmptyList() {
        // Response has "included" but with an empty "service_instances" list
        String responseJson = "{\"resources\":[{\"guid\":\"" + UUID.randomUUID() + "\"}],"
            + "\"included\":{\"service_instances\":[]},"
            + "\"pagination\":{\"next\":null}}";
        stubWebClientToReturnResponse(responseJson);

        UUID appGuid = UUID.randomUUID();
        List<String> result = client.getServiceInstanceNamesBoundToApp(appGuid);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetServiceInstanceNamesBoundToAppDeduplicatesAcrossPages() {
        // Same service name appears on both pages
        String page1Json = buildResponseWithIncludedServiceInstances(
            List.of("shared-service"), "/v3/service_credential_bindings?page=2");
        String page2Json = buildResponseWithIncludedServiceInstances(List.of("shared-service"), null);

        stubWebClientToReturnResponse(page1Json, page2Json);

        UUID appGuid = UUID.randomUUID();
        List<String> result = client.getServiceInstanceNamesBoundToApp(appGuid);

        assertEquals(1, result.size(), "Duplicate service names across pages should be deduplicated");
        assertEquals("shared-service", result.getFirst());
    }

    private String buildResponseWithIncludedServiceInstances(List<String> serviceNames, String nextPageHref) {
        List<UUID> serviceInstanceGuids = serviceNames.stream()
                                                      .map(service -> UUID.randomUUID())
                                                      .toList();
        String bindingResourcesJson = buildBindingResourcesJson(serviceInstanceGuids);
        String serviceInstancesJson = buildServiceInstanceResourcesJson(serviceNames, serviceInstanceGuids);
        String paginationJson = buildPaginationJson(nextPageHref);

        return "{\"resources\":[" + bindingResourcesJson + "],"
            + "\"included\":{\"service_instances\":[" + serviceInstancesJson + "]},"
            + "\"pagination\":" + paginationJson + "}";
    }

    private String buildBindingResourcesJson(List<UUID> serviceInstanceGuids) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < serviceInstanceGuids.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(buildSingleBindingResourceJson(serviceInstanceGuids.get(i)));
        }
        return sb.toString();
    }

    private String buildSingleBindingResourceJson(UUID serviceInstanceGuid) {
        return "{\"guid\":\"" + UUID.randomUUID() + "\","
            + "\"type\":\"app\","
            + "\"relationships\":{"
            + "\"service_instance\":{\"data\":{\"guid\":\"" + serviceInstanceGuid + "\"}}"
            + "}}";
    }

    private String buildServiceInstanceResourcesJson(List<String> serviceNames, List<UUID> serviceInstanceGuids) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < serviceNames.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(buildSingleServiceInstanceJson(serviceNames.get(i), serviceInstanceGuids.get(i)));
        }
        return sb.toString();
    }

    private String buildSingleServiceInstanceJson(String serviceName, UUID serviceInstanceGuid) {
        return "{\"guid\":\"" + serviceInstanceGuid + "\","
            + "\"name\":\"" + serviceName + "\","
            + "\"type\":\"managed\"}";
    }

    private String buildPaginationJson(String nextPageHref) {
        String nextPage = nextPageHref == null ? "null" : "{\"href\":\"" + nextPageHref + "\"}";
        return "{\"next\":" + nextPage + "}";
    }
}

