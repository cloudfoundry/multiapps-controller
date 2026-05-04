package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.CloudCredentials;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ServiceRouteBinding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceInstanceRoutesGetterTest extends CustomControllerClientBaseTest {

    private static final String CORRELATION_ID = "test-correlation-id";

    private ServiceInstanceRoutesGetter client;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        Mockito.when(applicationConfiguration.getVersion())
               .thenReturn("1.0.0");
        Mockito.when(webClientFactory.getWebClient(Mockito.any(CloudCredentials.class)))
               .thenReturn(webClient);

        client = new ServiceInstanceRoutesGetter(applicationConfiguration, webClientFactory, new CloudCredentials("user", "pass"),
                                                 CORRELATION_ID);
    }

    @Test
    void testGetServiceRouteBindingsWithEmptyGuidsReturnsEmptyList() {
        List<ServiceRouteBinding> result = client.getServiceRouteBindings(Collections.emptyList());
        assertTrue(result.isEmpty());
        Mockito.verifyNoInteractions(webClient);
    }

    @Test
    void testGetServiceRouteBindingsBuildsCorrectUri() {
        stubWebClientToReturnResponse();

        String routeGuid = UUID.randomUUID()
                               .toString();
        client.getServiceRouteBindings(List.of(routeGuid));

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(requestHeadersUriSpec)
               .uri(uriCaptor.capture());

        String capturedUri = uriCaptor.getValue();
        assertTrue(capturedUri.startsWith("/v3/service_route_bindings?"),
                   "URI should start with the service route bindings base path");
        assertTrue(capturedUri.contains("route_guids=" + routeGuid),
                   "URI should contain the route_guids parameter with the provided guid");
    }

    @Test
    void testGetServiceRouteBindingsJoinsMultipleGuidsWithComma() {
        stubWebClientToReturnResponse();

        String routeGuid1 = UUID.randomUUID()
                                .toString();
        String routeGuid2 = UUID.randomUUID()
                                .toString();
        client.getServiceRouteBindings(List.of(routeGuid1, routeGuid2));

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(requestHeadersUriSpec)
               .uri(uriCaptor.capture());

        String capturedUri = uriCaptor.getValue();
        assertTrue(capturedUri.contains("route_guids=" + routeGuid1 + "," + routeGuid2),
                   "URI should contain both guids joined by comma");
    }

    @Test
    void testGetServiceRouteBindingsReturnsMappedBindings() {
        String routeGuid = UUID.randomUUID()
                               .toString();
        String serviceInstanceGuid = UUID.randomUUID()
                                         .toString();

        String responseJson = buildServiceRouteBindingsResponse(routeGuid, serviceInstanceGuid, null);
        stubWebClientToReturnResponse(responseJson);

        List<ServiceRouteBinding> result = client.getServiceRouteBindings(List.of(routeGuid));

        assertEquals(1, result.size());
        ServiceRouteBinding binding = result.getFirst();
        assertEquals(routeGuid, binding.getRouteId());
        assertEquals(serviceInstanceGuid, binding.getServiceInstanceId());
    }

    @Test
    void testGetServiceRouteBindingsWithEmptyResponseReturnsEmptyList() {
        stubWebClientToReturnResponse();

        String routeGuid = UUID.randomUUID()
                               .toString();
        List<ServiceRouteBinding> result = client.getServiceRouteBindings(List.of(routeGuid));
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetServiceRouteBindingsReturnsMultipleBindings() {
        String routeGuid1 = UUID.randomUUID()
                                .toString();
        String routeGuid2 = UUID.randomUUID()
                                .toString();
        String serviceInstanceGuid1 = UUID.randomUUID()
                                          .toString();
        String serviceInstanceGuid2 = UUID.randomUUID()
                                          .toString();

        String responseJson = buildMultipleServiceRouteBindingsResponse(
            List.of(routeGuid1, routeGuid2),
            List.of(serviceInstanceGuid1, serviceInstanceGuid2));
        stubWebClientToReturnResponse(responseJson);

        List<ServiceRouteBinding> result = client.getServiceRouteBindings(List.of(routeGuid1, routeGuid2));

        assertEquals(2, result.size());
        assertEquals(routeGuid1, result.getFirst()
                                       .getRouteId());
        assertEquals(serviceInstanceGuid1, result.getFirst()
                                                 .getServiceInstanceId());
        assertEquals(routeGuid2, result.get(1)
                                       .getRouteId());
        assertEquals(serviceInstanceGuid2, result.get(1)
                                                 .getServiceInstanceId());
    }

    @Test
    void testGetServiceRouteBindingsMultipleBindingsForSameRoute() {
        String routeGuid = UUID.randomUUID()
                               .toString();
        String serviceInstanceGuid1 = UUID.randomUUID()
                                          .toString();
        String serviceInstanceGuid2 = UUID.randomUUID()
                                          .toString();

        String responseJson = buildMultipleServiceRouteBindingsResponse(
            List.of(routeGuid, routeGuid),
            List.of(serviceInstanceGuid1, serviceInstanceGuid2));
        stubWebClientToReturnResponse(responseJson);

        List<ServiceRouteBinding> result = client.getServiceRouteBindings(List.of(routeGuid));

        assertEquals(2, result.size());
        assertEquals(routeGuid, result.getFirst()
                                      .getRouteId());
        assertEquals(serviceInstanceGuid1, result.getFirst()
                                                 .getServiceInstanceId());
        assertEquals(routeGuid, result.get(1)
                                      .getRouteId());
        assertEquals(serviceInstanceGuid2, result.get(1)
                                                 .getServiceInstanceId());
    }

    @Test
    void testGetServiceRouteBindingsPaginatedResponseFollowsAllPages() {
        String routeGuid = UUID.randomUUID()
                               .toString();
        String serviceInstanceGuid1 = UUID.randomUUID()
                                          .toString();
        String serviceInstanceGuid2 = UUID.randomUUID()
                                          .toString();

        String page1Json = buildServiceRouteBindingsResponse(routeGuid, serviceInstanceGuid1,
                                                             "/v3/service_route_bindings?page=2");
        String page2Json = buildServiceRouteBindingsResponse(routeGuid, serviceInstanceGuid2, null);

        stubWebClientToReturnResponse(page1Json, page2Json);

        List<ServiceRouteBinding> result = client.getServiceRouteBindings(List.of(routeGuid));

        assertEquals(2, result.size());
        assertEquals(serviceInstanceGuid1, result.getFirst()
                                                 .getServiceInstanceId());
        assertEquals(serviceInstanceGuid2, result.get(1)
                                                 .getServiceInstanceId());
    }

    @Test
    void testBatchingTriggeredWithManyGuids() {
        stubWebClientToReturnResponse();

        List<String> manyGuids = generateRandomGuids(200);

        client.getServiceRouteBindings(manyGuids);

        Mockito.verify(webClient, Mockito.atLeast(2))
               .get();
    }

    @Test
    void testSingleBatchWithFewGuids() {
        stubWebClientToReturnResponse();

        List<String> fewGuids = generateRandomGuids(3);

        client.getServiceRouteBindings(fewGuids);

        Mockito.verify(webClient, Mockito.times(1))
               .get();
    }

    @Test
    void testBatchedUrisNeverExceedMaxLength() {
        stubWebClientToReturnResponse();

        List<String> manyGuids = generateRandomGuids(800);
        client.getServiceRouteBindings(manyGuids);

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(requestHeadersUriSpec, Mockito.atLeastOnce())
               .uri(uriCaptor.capture());

        for (String uri : uriCaptor.getAllValues()) {
            assertTrue(uri.length() <= CustomControllerClient.MAX_URI_QUERY_LENGTH,
                       "URI length " + uri.length() + " exceeds MAX_URI_QUERY_LENGTH "
                           + CustomControllerClient.MAX_URI_QUERY_LENGTH);
        }
    }

    @Test
    void testBatchedRequestsContainAllGuidsInOrder() {
        stubWebClientToReturnResponse();

        List<String> manyGuids = generateRandomGuids(200);
        client.getServiceRouteBindings(manyGuids);

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(requestHeadersUriSpec, Mockito.atLeastOnce())
               .uri(uriCaptor.capture());

        List<String> allCapturedGuids = new ArrayList<>();
        for (String uri : uriCaptor.getAllValues()) {
            int idx = uri.indexOf("route_guids=");
            assertTrue(idx >= 0, "URI should contain route_guids=");
            String guidsStr = uri.substring(idx + "route_guids=".length());
            String[] guids = guidsStr.split(",");
            Collections.addAll(allCapturedGuids, guids);
        }

        assertEquals(manyGuids.size(), allCapturedGuids.size(), "All guids must be sent across batched requests");
        assertEquals(manyGuids, allCapturedGuids, "Guids must appear in original order across batches");
    }

    @Test
    void testBatchedRequestsAggregateResultsFromAllBatches() {
        String routeGuid1 = UUID.randomUUID()
                                .toString();
        String serviceInstanceGuid1 = UUID.randomUUID()
                                          .toString();

        String response = buildServiceRouteBindingsResponse(routeGuid1, serviceInstanceGuid1, null);
        stubWebClientToReturnResponse(response);

        List<ServiceRouteBinding> result = client.getServiceRouteBindings(List.of(routeGuid1));

        assertEquals(1, result.size());
        assertEquals(routeGuid1, result.getFirst()
                                       .getRouteId());
        assertEquals(serviceInstanceGuid1, result.getFirst()
                                                 .getServiceInstanceId());
    }

    private List<String> generateRandomGuids(int count) {
        List<String> guids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            guids.add(UUID.randomUUID()
                          .toString());
        }
        return guids;
    }

    private String buildServiceRouteBindingsResponse(String routeGuid, String serviceInstanceGuid, String nextPageHref) {
        String resourceJson = buildRouteBindingResourceJson(routeGuid, serviceInstanceGuid);
        return assembleRouteBindingsResponseJson(resourceJson, buildPaginationJson(nextPageHref));
    }

    private String buildRouteBindingResourceJson(String routeGuid, String serviceInstanceGuid) {
        return "{\"guid\":\"" + UUID.randomUUID() + "\","
            + "\"created_at\":\"2024-01-01T00:00:00Z\","
            + "\"updated_at\":\"2024-01-01T00:00:00Z\","
            + "\"relationships\":{"
            + "\"route\":{\"data\":{\"guid\":\"" + routeGuid + "\"}},"
            + "\"service_instance\":{\"data\":{\"guid\":\"" + serviceInstanceGuid + "\"}}"
            + "}}";
    }

    private String buildPaginationJson(String nextPageHref) {
        String nextPage = nextPageHref == null ? "null" : "{\"href\":\"" + nextPageHref + "\"}";
        return "{\"next\":" + nextPage + "}";
    }

    private String assembleRouteBindingsResponseJson(String resourcesJson, String paginationJson) {
        return "{\"resources\":[" + resourcesJson + "],"
            + "\"pagination\":" + paginationJson + "}";
    }

    private String buildMultipleServiceRouteBindingsResponse(List<String> routeGuids, List<String> serviceInstanceGuids) {
        String resourcesJson = buildMultipleRouteBindingResourcesJson(routeGuids, serviceInstanceGuids);
        return assembleRouteBindingsResponseJson(resourcesJson, buildPaginationJson(null));
    }

    private String buildMultipleRouteBindingResourcesJson(List<String> routeGuids, List<String> serviceInstanceGuids) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < routeGuids.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(buildRouteBindingResourceJson(routeGuids.get(i), serviceInstanceGuids.get(i)));
        }
        return sb.toString();
    }
}




