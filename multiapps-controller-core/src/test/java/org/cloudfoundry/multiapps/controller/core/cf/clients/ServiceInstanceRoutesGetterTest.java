package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.cloudfoundry.multiapps.controller.client.facade.CloudCredentials;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ServiceRouteBinding;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceInstanceRoutesGetterTest {

    private static final String CORRELATION_ID = "test-correlation-id";

    @Mock
    private WebClientFactory webClientFactory;
    @Mock
    private ApplicationConfiguration applicationConfiguration;
    @Mock
    private WebClient webClient;

    @SuppressWarnings("rawtypes")
    private final WebClient.RequestHeadersUriSpec requestHeadersUriSpec = Mockito.mock(WebClient.RequestHeadersUriSpec.class);
    @SuppressWarnings("rawtypes")
    private final WebClient.RequestHeadersSpec requestHeadersSpec = Mockito.mock(WebClient.RequestHeadersSpec.class);
    @Mock
    private WebClient.ResponseSpec responseSpec;

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
        stubWebClientToReturnEmptyPage();

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
        stubWebClientToReturnEmptyPage();

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

        String responseJson = buildServiceRouteBindingsResponse(routeGuid, serviceInstanceGuid);
        stubWebClientToReturn(responseJson);

        List<ServiceRouteBinding> result = client.getServiceRouteBindings(List.of(routeGuid));

        assertEquals(1, result.size());
        ServiceRouteBinding binding = result.getFirst();
        assertEquals(routeGuid, binding.getRouteId());
        assertEquals(serviceInstanceGuid, binding.getServiceInstanceId());
    }

    @Test
    void testGetServiceRouteBindingsWithEmptyResponseReturnsEmptyList() {
        stubWebClientToReturnEmptyPage();

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
        stubWebClientToReturn(responseJson);

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
        stubWebClientToReturn(responseJson);

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

        String page1Json = buildServiceRouteBindingsResponseWithPagination(routeGuid, serviceInstanceGuid1,
                                                                           "/v3/service_route_bindings?page=2");
        String page2Json = buildServiceRouteBindingsResponse(routeGuid, serviceInstanceGuid2);

        stubWebClientToReturnSequentially(page1Json, page2Json);

        List<ServiceRouteBinding> result = client.getServiceRouteBindings(List.of(routeGuid));

        assertEquals(2, result.size());
        assertEquals(serviceInstanceGuid1, result.getFirst()
                                                 .getServiceInstanceId());
        assertEquals(serviceInstanceGuid2, result.get(1)
                                                 .getServiceInstanceId());
    }

    @Test
    void testBatchingTriggeredWithManyGuids() {
        stubWebClientToReturnEmptyPage();

        List<String> manyGuids = generateRandomGuids(200);

        client.getServiceRouteBindings(manyGuids);

        Mockito.verify(webClient, Mockito.atLeast(2))
               .get();
    }

    @Test
    void testSingleBatchWithFewGuids() {
        stubWebClientToReturnEmptyPage();

        List<String> fewGuids = generateRandomGuids(3);

        client.getServiceRouteBindings(fewGuids);

        Mockito.verify(webClient, Mockito.times(1))
               .get();
    }

    @Test
    void testBatchedUrisNeverExceedMaxLength() {
        stubWebClientToReturnEmptyPage();

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
        stubWebClientToReturnEmptyPage();

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

        String response = buildServiceRouteBindingsResponse(routeGuid1, serviceInstanceGuid1);
        stubWebClientToReturn(response);

        List<ServiceRouteBinding> result = client.getServiceRouteBindings(List.of(routeGuid1));

        assertEquals(1, result.size());
        assertEquals(routeGuid1, result.getFirst()
                                       .getRouteId());
        assertEquals(serviceInstanceGuid1, result.getFirst()
                                                 .getServiceInstanceId());
    }

    @SuppressWarnings("unchecked")
    private void stubWebClientToReturnEmptyPage() {
        Mockito.when(webClient.get())
               .thenReturn(requestHeadersUriSpec);
        Mockito.when(requestHeadersUriSpec.uri(Mockito.anyString()))
               .thenReturn(requestHeadersSpec);
        Mockito.when(requestHeadersSpec.headers(Mockito.any(Consumer.class)))
               .thenReturn(requestHeadersSpec);
        Mockito.when(requestHeadersSpec.retrieve())
               .thenReturn(responseSpec);
        Mockito.when(responseSpec.bodyToMono(String.class))
               .thenReturn(Mono.just("{\"resources\":[],\"pagination\":{\"next\":null}}"));
    }

    @SuppressWarnings("unchecked")
    private void stubWebClientToReturn(String responseJson) {
        Mockito.when(webClient.get())
               .thenReturn(requestHeadersUriSpec);
        Mockito.when(requestHeadersUriSpec.uri(Mockito.anyString()))
               .thenReturn(requestHeadersSpec);
        Mockito.when(requestHeadersSpec.headers(Mockito.any(Consumer.class)))
               .thenReturn(requestHeadersSpec);
        Mockito.when(requestHeadersSpec.retrieve())
               .thenReturn(responseSpec);
        Mockito.when(responseSpec.bodyToMono(String.class))
               .thenReturn(Mono.just(responseJson));
    }

    @SuppressWarnings("unchecked")
    private void stubWebClientToReturnSequentially(String... responses) {
        Mockito.when(webClient.get())
               .thenReturn(requestHeadersUriSpec);
        Mockito.when(requestHeadersUriSpec.uri(Mockito.anyString()))
               .thenReturn(requestHeadersSpec);
        Mockito.when(requestHeadersSpec.headers(Mockito.any(Consumer.class)))
               .thenReturn(requestHeadersSpec);
        Mockito.when(requestHeadersSpec.retrieve())
               .thenReturn(responseSpec);

        if (responses.length == 1) {
            Mockito.when(responseSpec.bodyToMono(String.class))
                   .thenReturn(Mono.just(responses[0]));
        } else {
            @SuppressWarnings("rawtypes") Mono[] remaining = new Mono[responses.length - 1];
            for (int i = 1; i < responses.length; i++) {
                remaining[i - 1] = Mono.just(responses[i]);
            }
            Mockito.when(responseSpec.bodyToMono(String.class))
                   .thenReturn(Mono.just(responses[0]), remaining);
        }
    }

    private List<String> generateRandomGuids(int count) {
        List<String> guids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            guids.add(UUID.randomUUID()
                          .toString());
        }
        return guids;
    }

    private String buildServiceRouteBindingsResponse(String routeGuid, String serviceInstanceGuid) {
        return buildServiceRouteBindingsResponseWithPagination(routeGuid, serviceInstanceGuid, null);
    }

    private String buildServiceRouteBindingsResponseWithPagination(String routeGuid, String serviceInstanceGuid, String nextPageHref) {
        String nextPage = nextPageHref == null ? "null" : "{\"href\":\"" + nextPageHref + "\"}";
        return "{"
            + "\"resources\":["
            + "  {"
            + "    \"guid\":\"" + UUID.randomUUID() + "\","
            + "    \"created_at\":\"2024-01-01T00:00:00Z\","
            + "    \"updated_at\":\"2024-01-01T00:00:00Z\","
            + "    \"relationships\":{"
            + "      \"route\":{\"data\":{\"guid\":\"" + routeGuid + "\"}},"
            + "      \"service_instance\":{\"data\":{\"guid\":\"" + serviceInstanceGuid + "\"}}"
            + "    }"
            + "  }"
            + "],"
            + "\"pagination\":{\"next\":" + nextPage + "}"
            + "}";
    }

    private String buildMultipleServiceRouteBindingsResponse(List<String> routeGuids, List<String> serviceInstanceGuids) {
        StringBuilder resources = new StringBuilder();
        for (int i = 0; i < routeGuids.size(); i++) {
            if (i > 0) {
                resources.append(",");
            }
            resources.append("{")
                     .append("\"guid\":\"")
                     .append(UUID.randomUUID())
                     .append("\",")
                     .append("\"created_at\":\"2024-01-01T00:00:00Z\",")
                     .append("\"updated_at\":\"2024-01-01T00:00:00Z\",")
                     .append("\"relationships\":{")
                     .append("  \"route\":{\"data\":{\"guid\":\"")
                     .append(routeGuids.get(i))
                     .append("\"}},")
                     .append("  \"service_instance\":{\"data\":{\"guid\":\"")
                     .append(serviceInstanceGuids.get(i))
                     .append("\"}}")
                     .append("}")
                     .append("}");
        }

        return "{"
            + "\"resources\":[" + resources + "],"
            + "\"pagination\":{\"next\":null}"
            + "}";
    }
}




