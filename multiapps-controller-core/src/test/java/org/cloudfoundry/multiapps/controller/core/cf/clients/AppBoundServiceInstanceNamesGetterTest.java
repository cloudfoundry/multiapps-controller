package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.cloudfoundry.multiapps.controller.client.facade.CloudCredentials;
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

class AppBoundServiceInstanceNamesGetterTest {

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
        stubWebClientToReturnEmptyPage();

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
        stubWebClientToReturnEmptyPage();

        UUID appGuid = UUID.randomUUID();
        List<String> result = client.getServiceInstanceNamesBoundToApp(appGuid);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetServiceInstanceNamesBoundToAppReturnsSingleServiceName() {
        String serviceName = "my-database";
        String responseJson = buildResponseWithIncludedServiceInstances(List.of(serviceName));
        stubWebClientToReturn(responseJson);

        UUID appGuid = UUID.randomUUID();
        List<String> result = client.getServiceInstanceNamesBoundToApp(appGuid);

        assertEquals(1, result.size());
        assertEquals(serviceName, result.getFirst());
    }

    @Test
    void testGetServiceInstanceNamesBoundToAppReturnsMultipleServiceNames() {
        List<String> serviceNames = List.of("db-service", "cache-service", "queue-service");
        String responseJson = buildResponseWithIncludedServiceInstances(serviceNames);
        stubWebClientToReturn(responseJson);

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
        String responseJson = buildResponseWithIncludedServiceInstances(serviceNames);
        stubWebClientToReturn(responseJson);

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
        stubWebClientToReturn(responseJson);

        UUID appGuid = UUID.randomUUID();
        List<String> result = client.getServiceInstanceNamesBoundToApp(appGuid);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetServiceInstanceNamesBoundToAppPaginatedResponseFollowsAllPages() {
        String page1Json = buildResponseWithIncludedServiceInstancesAndPagination(
            List.of("svc-page1"), "/v3/service_credential_bindings?page=2");
        String page2Json = buildResponseWithIncludedServiceInstances(List.of("svc-page2"));

        stubWebClientToReturnSequentially(page1Json, page2Json);

        UUID appGuid = UUID.randomUUID();
        List<String> result = client.getServiceInstanceNamesBoundToApp(appGuid);

        assertEquals(2, result.size());
        assertEquals("svc-page1", result.get(0));
        assertEquals("svc-page2", result.get(1));
    }

    @Test
    void testGetServiceInstanceNamesBoundToAppMakesExactlyOneHttpCallForSinglePage() {
        stubWebClientToReturnEmptyPage();

        UUID appGuid = UUID.randomUUID();
        client.getServiceInstanceNamesBoundToApp(appGuid);

        Mockito.verify(webClient, Mockito.times(1))
               .get();
    }

    @Test
    void testGetServiceInstanceNamesBoundToAppMakesTwoHttpCallsForTwoPages() {
        String page1Json = buildResponseWithIncludedServiceInstancesAndPagination(
            List.of("svc-1"), "/v3/service_credential_bindings?page=2");
        String page2Json = buildResponseWithIncludedServiceInstances(List.of("svc-2"));

        stubWebClientToReturnSequentially(page1Json, page2Json);

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
        stubWebClientToReturn(responseJson);

        UUID appGuid = UUID.randomUUID();
        List<String> result = client.getServiceInstanceNamesBoundToApp(appGuid);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetServiceInstanceNamesBoundToAppDeduplicatesAcrossPages() {
        // Same service name appears on both pages
        String page1Json = buildResponseWithIncludedServiceInstancesAndPagination(
            List.of("shared-service"), "/v3/service_credential_bindings?page=2");
        String page2Json = buildResponseWithIncludedServiceInstances(List.of("shared-service"));

        stubWebClientToReturnSequentially(page1Json, page2Json);

        UUID appGuid = UUID.randomUUID();
        List<String> result = client.getServiceInstanceNamesBoundToApp(appGuid);

        assertEquals(1, result.size(), "Duplicate service names across pages should be deduplicated");
        assertEquals("shared-service", result.getFirst());
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

    private String buildResponseWithIncludedServiceInstances(List<String> serviceNames) {
        return buildResponseWithIncludedServiceInstancesAndPagination(serviceNames, null);
    }

    private String buildResponseWithIncludedServiceInstancesAndPagination(List<String> serviceNames, String nextPageHref) {
        String nextPage = nextPageHref == null ? "null" : "{\"href\":\"" + nextPageHref + "\"}";

        StringBuilder bindingResources = new StringBuilder();
        StringBuilder serviceInstanceResources = new StringBuilder();

        for (int i = 0; i < serviceNames.size(); i++) {
            UUID serviceInstanceGuid = UUID.randomUUID();

            if (i > 0) {
                bindingResources.append(",");
                serviceInstanceResources.append(",");
            }

            bindingResources.append("{")
                            .append("\"guid\":\"")
                            .append(UUID.randomUUID())
                            .append("\",")
                            .append("\"type\":\"app\",")
                            .append("\"relationships\":{")
                            .append("  \"service_instance\":{\"data\":{\"guid\":\"")
                            .append(serviceInstanceGuid)
                            .append("\"}}")
                            .append("}")
                            .append("}");

            serviceInstanceResources.append("{")
                                    .append("\"guid\":\"")
                                    .append(serviceInstanceGuid)
                                    .append("\",")
                                    .append("\"name\":\"")
                                    .append(serviceNames.get(i))
                                    .append("\",")
                                    .append("\"type\":\"managed\"")
                                    .append("}");
        }

        return "{\"resources\":[" + bindingResources + "],"
            + "\"included\":{\"service_instances\":[" + serviceInstanceResources + "]},"
            + "\"pagination\":{\"next\":" + nextPage + "}}";
    }
}

