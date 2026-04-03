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

class CFOptimizedEventGetterTest {

    private static final String EVENT_TYPE = "audit.app.update";
    private static final String TIMESTAMP = "2024-06-01T00:00:00Z";

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

    private CFOptimizedEventGetter client;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        Mockito.when(applicationConfiguration.getVersion())
               .thenReturn("1.0.0");
        Mockito.when(webClientFactory.getWebClient(Mockito.any(CloudCredentials.class)))
               .thenReturn(webClient);

        client = new CFOptimizedEventGetter(applicationConfiguration, webClientFactory, new CloudCredentials("user", "pass"));
    }

    @Test
    void testFindEventsBuildsCorrectUri() {
        stubWebClientToReturnEmptyPage();

        client.findEvents(EVENT_TYPE, TIMESTAMP);

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(requestHeadersUriSpec)
               .uri(uriCaptor.capture());

        String capturedUri = uriCaptor.getValue();
        assertTrue(capturedUri.startsWith("/v3/audit_events?"), "URI should start with the audit events base path");
        assertTrue(capturedUri.contains("types=" + EVENT_TYPE), "URI should contain the event type");
        assertTrue(capturedUri.contains("per_page=100"), "URI should contain per_page=100");
        assertTrue(capturedUri.contains("created_ats[gt]=" + TIMESTAMP), "URI should contain the timestamp filter");
    }

    @Test
    void testFindEventsWithEmptyResponseReturnsEmptyList() {
        stubWebClientToReturnEmptyPage();

        List<String> result = client.findEvents(EVENT_TYPE, TIMESTAMP);

        assertTrue(result.isEmpty());
    }

    @Test
    void testFindEventsReturnsSingleSpaceId() {
        String spaceGuid = UUID.randomUUID()
                               .toString();
        String responseJson = buildAuditEventsResponse(List.of(spaceGuid));
        stubWebClientToReturn(responseJson);

        List<String> result = client.findEvents(EVENT_TYPE, TIMESTAMP);

        assertEquals(1, result.size());
        assertEquals(spaceGuid, result.getFirst());
    }

    @Test
    void testFindEventsReturnsMultipleSpaceIds() {
        String spaceGuid1 = UUID.randomUUID()
                                .toString();
        String spaceGuid2 = UUID.randomUUID()
                                .toString();
        String spaceGuid3 = UUID.randomUUID()
                                .toString();
        String responseJson = buildAuditEventsResponse(List.of(spaceGuid1, spaceGuid2, spaceGuid3));
        stubWebClientToReturn(responseJson);

        List<String> result = client.findEvents(EVENT_TYPE, TIMESTAMP);

        assertEquals(3, result.size());
        assertEquals(spaceGuid1, result.get(0));
        assertEquals(spaceGuid2, result.get(1));
        assertEquals(spaceGuid3, result.get(2));
    }

    @Test
    void testFindEventsFiltersNullSpaceGuids() {
        String spaceGuid = UUID.randomUUID()
                               .toString();
        // Build a response with one event having a valid space guid and one with null guid
        String responseJson = buildAuditEventsResponseWithNullSpaceGuid(spaceGuid);
        stubWebClientToReturn(responseJson);

        List<String> result = client.findEvents(EVENT_TYPE, TIMESTAMP);

        assertEquals(1, result.size());
        assertEquals(spaceGuid, result.getFirst());
    }

    @Test
    void testFindEventsPaginatedResponseFollowsAllPages() {
        String spaceGuid1 = UUID.randomUUID()
                                .toString();
        String spaceGuid2 = UUID.randomUUID()
                                .toString();

        String page1Json = buildAuditEventsResponseWithPagination(List.of(spaceGuid1), "/v3/audit_events?page=2");
        String page2Json = buildAuditEventsResponse(List.of(spaceGuid2));

        stubWebClientToReturnSequentially(page1Json, page2Json);

        List<String> result = client.findEvents(EVENT_TYPE, TIMESTAMP);

        assertEquals(2, result.size());
        assertEquals(spaceGuid1, result.getFirst());
        assertEquals(spaceGuid2, result.get(1));
    }

    @Test
    void testFindEventsMakesExactlyOneHttpCallForSinglePage() {
        stubWebClientToReturnEmptyPage();

        client.findEvents(EVENT_TYPE, TIMESTAMP);

        Mockito.verify(webClient, Mockito.times(1))
               .get();
    }

    @Test
    void testFindEventsMakesTwoHttpCallsForTwoPages() {
        String spaceGuid = UUID.randomUUID()
                               .toString();

        String page1Json = buildAuditEventsResponseWithPagination(List.of(spaceGuid), "/v3/audit_events?page=2");
        String page2Json = buildAuditEventsResponse(List.of(spaceGuid));

        stubWebClientToReturnSequentially(page1Json, page2Json);

        client.findEvents(EVENT_TYPE, TIMESTAMP);

        Mockito.verify(webClient, Mockito.times(2))
               .get();
    }

    @Test
    void testFindEventsWithDifferentEventType() {
        stubWebClientToReturnEmptyPage();

        String customType = "audit.app.delete";
        client.findEvents(customType, TIMESTAMP);

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(requestHeadersUriSpec)
               .uri(uriCaptor.capture());

        String capturedUri = uriCaptor.getValue();
        assertTrue(capturedUri.contains("types=" + customType), "URI should contain the custom event type");
    }

    @Test
    void testFindEventsWithDifferentTimestamp() {
        stubWebClientToReturnEmptyPage();

        String customTimestamp = "2025-12-31T23:59:59Z";
        client.findEvents(EVENT_TYPE, customTimestamp);

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(requestHeadersUriSpec)
               .uri(uriCaptor.capture());

        String capturedUri = uriCaptor.getValue();
        assertTrue(capturedUri.contains("created_ats[gt]=" + customTimestamp), "URI should contain the custom timestamp");
    }

    @Test
    void testFindEventsDuplicateSpaceIdsArePreserved() {
        String spaceGuid = UUID.randomUUID()
                               .toString();
        // Same space guid appearing in multiple events
        String responseJson = buildAuditEventsResponse(List.of(spaceGuid, spaceGuid, spaceGuid));
        stubWebClientToReturn(responseJson);

        List<String> result = client.findEvents(EVENT_TYPE, TIMESTAMP);

        assertEquals(3, result.size(), "Duplicate space IDs should be preserved since the mapper does not deduplicate");
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

    private String buildAuditEventsResponse(List<String> spaceGuids) {
        return buildAuditEventsResponseWithPagination(spaceGuids, null);
    }

    private String buildAuditEventsResponseWithPagination(List<String> spaceGuids, String nextPageHref) {
        String nextPage = nextPageHref == null ? "null" : "{\"href\":\"" + nextPageHref + "\"}";
        StringBuilder resources = new StringBuilder();
        for (int i = 0; i < spaceGuids.size(); i++) {
            if (i > 0) {
                resources.append(",");
            }
            resources.append("{")
                     .append("\"guid\":\"")
                     .append(UUID.randomUUID())
                     .append("\",")
                     .append("\"type\":\"audit.app.update\",")
                     .append("\"created_at\":\"2024-06-15T10:00:00Z\",")
                     .append("\"space\":{\"guid\":\"")
                     .append(spaceGuids.get(i))
                     .append("\"}")
                     .append("}");
        }
        return "{\"resources\":[" + resources + "],\"pagination\":{\"next\":" + nextPage + "}}";
    }

    private String buildAuditEventsResponseWithNullSpaceGuid(String validSpaceGuid) {
        return "{\"resources\":["
            + "{\"guid\":\"" + UUID.randomUUID() + "\",\"type\":\"audit.app.update\","
            + "\"created_at\":\"2024-06-15T10:00:00Z\","
            + "\"space\":{\"guid\":\"" + validSpaceGuid + "\"}},"
            + "{\"guid\":\"" + UUID.randomUUID() + "\",\"type\":\"audit.app.update\","
            + "\"created_at\":\"2024-06-15T10:00:00Z\","
            + "\"space\":{\"guid\":null}}"
            + "],\"pagination\":{\"next\":null}}";
    }
}

