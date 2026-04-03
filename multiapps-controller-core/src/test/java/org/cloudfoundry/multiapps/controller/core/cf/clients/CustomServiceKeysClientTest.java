package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.cloudfoundry.client.v3.serviceinstances.ServiceInstanceType;
import org.cloudfoundry.multiapps.controller.client.facade.CloudCredentials;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudMetadata;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaService;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaServiceKey;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMtaService;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomServiceKeysClientTest {

    private static final String SPACE_GUID = "space-guid-123";
    private static final String MTA_ID = "my-mta";
    private static final String MTA_NAMESPACE = "my-namespace";
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

    private CustomServiceKeysClient client;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        Mockito.when(applicationConfiguration.getVersion())
               .thenReturn("1.0.0");
        Mockito.when(webClientFactory.getWebClient(Mockito.any(CloudCredentials.class)))
               .thenReturn(webClient);

        client = new CustomServiceKeysClient(applicationConfiguration, webClientFactory, new CloudCredentials("user", "pass"),
                                             CORRELATION_ID);
    }

    @Test
    void testGetServiceKeysByExistingGuidsWithEmptyGuidsReturnsEmptyList() {
        List<DeployedMtaServiceKey> result = client.getServiceKeysByMetadataAndExistingGuids(SPACE_GUID, MTA_ID, MTA_NAMESPACE,
                                                                                             Collections.emptyList());
        assertTrue(result.isEmpty());
        Mockito.verifyNoInteractions(webClient);
    }

    @Test
    void testGetServiceKeysByExistingGuidsWithAllNullGuidsReturnsEmptyList() {
        List<String> nullGuids = new ArrayList<>();
        nullGuids.add(null);
        nullGuids.add(null);

        List<DeployedMtaServiceKey> result = client.getServiceKeysByMetadataAndExistingGuids(SPACE_GUID, MTA_ID, MTA_NAMESPACE, nullGuids);
        assertTrue(result.isEmpty());
        Mockito.verifyNoInteractions(webClient);
    }

    @Test
    void testGetServiceKeysByExistingGuidsFiltersNullGuids() {
        stubWebClientToReturnEmptyPage();

        String randomServiceGuid = UUID.randomUUID()
                                       .toString();
        List<String> guidsWithNulls = new ArrayList<>();
        guidsWithNulls.add(null);
        guidsWithNulls.add(randomServiceGuid);
        guidsWithNulls.add(null);

        client.getServiceKeysByMetadataAndExistingGuids(SPACE_GUID, MTA_ID, MTA_NAMESPACE,
                                                        guidsWithNulls);

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(requestHeadersUriSpec)
               .uri(uriCaptor.capture());
        String capturedUri = uriCaptor.getValue();
        assertTrue(capturedUri.endsWith(MessageFormat.format("&service_instance_guids={0}", randomServiceGuid)),
                   "service_instance_guids should contain only the non-null guid");
    }

    @Test
    void testGetServiceKeysByExistingGuidsBuildsCorrectUri() {
        stubWebClientToReturnEmptyPage();

        String guid = UUID.randomUUID()
                          .toString();
        client.getServiceKeysByMetadataAndExistingGuids(SPACE_GUID, MTA_ID, MTA_NAMESPACE, List.of(guid));

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(requestHeadersUriSpec)
               .uri(uriCaptor.capture());

        String capturedUri = uriCaptor.getValue();
        assertTrue(capturedUri.startsWith("/v3/service_credential_bindings?type=key&label_selector="),
                   "URI should start with the service keys base path");
        assertTrue(capturedUri.contains("space_guid=" + SPACE_GUID), "URI should contain the space_guid label selector");
        assertTrue(capturedUri.contains("&include=service_instance"), "URI should include service_instance");
        assertTrue(capturedUri.contains("&service_instance_guids=" + guid),
                   "URI should contain the service instance guid parameter");
    }

    @Test
    void testGetServiceKeysByExistingGuidsJoinsMultipleGuidsWithComma() {
        stubWebClientToReturnEmptyPage();

        String guid1 = UUID.randomUUID()
                           .toString();
        String guid2 = UUID.randomUUID()
                           .toString();
        client.getServiceKeysByMetadataAndExistingGuids(SPACE_GUID, MTA_ID, MTA_NAMESPACE, List.of(guid1, guid2));

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(requestHeadersUriSpec)
               .uri(uriCaptor.capture());

        String capturedUri = uriCaptor.getValue();
        assertTrue(capturedUri.contains("&service_instance_guids=" + guid1 + "," + guid2),
                   "URI should contain both guids joined by comma");
    }

    @Test
    void testGetServiceKeysByExistingGuidsReturnsServiceKeys() {
        UUID serviceInstanceGuid = UUID.randomUUID();
        UUID serviceKeyGuid = UUID.randomUUID();

        String responseJson = buildServiceKeysResponse(serviceKeyGuid, serviceInstanceGuid, "my-key", "my-service");
        stubWebClientToReturn(responseJson);

        List<DeployedMtaServiceKey> result = client.getServiceKeysByMetadataAndExistingGuids(SPACE_GUID, MTA_ID, MTA_NAMESPACE,
                                                                                             List.of(serviceInstanceGuid.toString()));

        assertEquals(1, result.size());
        DeployedMtaServiceKey key = result.getFirst();
        assertEquals("my-key", key.getName());
        assertEquals(serviceKeyGuid, key.getMetadata()
                                        .getGuid());
        assertNotNull(key.getServiceInstance());
        assertEquals("my-service", key.getServiceInstance()
                                      .getName());
    }

    @Test
    void testGetServiceKeysByExistingGuidsWithEmptyResponseReturnsEmptyList() {
        stubWebClientToReturnEmptyPage();

        String guid = UUID.randomUUID()
                          .toString();
        List<DeployedMtaServiceKey> result = client.getServiceKeysByMetadataAndExistingGuids(SPACE_GUID, MTA_ID, MTA_NAMESPACE,
                                                                                             List.of(guid));
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetServiceKeysByManagedServicesWithEmptyServiceListReturnsEmptyList() {
        List<DeployedMtaServiceKey> result = client.getServiceKeysByMetadataAndManagedServices(SPACE_GUID, MTA_ID, MTA_NAMESPACE,
                                                                                               Collections.emptyList());
        assertTrue(result.isEmpty());
        Mockito.verifyNoInteractions(webClient);
    }

    @Test
    void testGetServiceKeysByManagedServicesWithOnlyUserProvidedReturnsEmptyList() {
        DeployedMtaService userProvidedService = ImmutableDeployedMtaService.builder()
                                                                            .name("ups")
                                                                            .type(ServiceInstanceType.USER_PROVIDED)
                                                                            .metadata(ImmutableCloudMetadata.builder()
                                                                                                            .guid(UUID.randomUUID())
                                                                                                            .build())
                                                                            .build();

        List<DeployedMtaServiceKey> result = client.getServiceKeysByMetadataAndManagedServices(SPACE_GUID, MTA_ID, MTA_NAMESPACE,
                                                                                               List.of(userProvidedService));
        assertTrue(result.isEmpty());
        Mockito.verifyNoInteractions(webClient);
    }

    @Test
    void testGetServiceKeysByManagedServicesWithNullMetadataReturnsEmptyList() {
        DeployedMtaService serviceWithNullMetadata = ImmutableDeployedMtaService.builder()
                                                                                .name("no-meta")
                                                                                .type(ServiceInstanceType.MANAGED)
                                                                                .build();

        List<DeployedMtaServiceKey> result = client.getServiceKeysByMetadataAndManagedServices(SPACE_GUID, MTA_ID, MTA_NAMESPACE,
                                                                                               List.of(serviceWithNullMetadata));
        assertTrue(result.isEmpty());
        Mockito.verifyNoInteractions(webClient);
    }

    @Test
    void testGetServiceKeysByManagedServicesWithNullGuidInMetadataReturnsEmptyList() {
        DeployedMtaService serviceWithNullGuid = ImmutableDeployedMtaService.builder()
                                                                            .name("null-guid")
                                                                            .type(ServiceInstanceType.MANAGED)
                                                                            .metadata(ImmutableCloudMetadata.builder()
                                                                                                            .build())
                                                                            .build();

        List<DeployedMtaServiceKey> result = client.getServiceKeysByMetadataAndManagedServices(SPACE_GUID, MTA_ID, MTA_NAMESPACE,
                                                                                               List.of(serviceWithNullGuid));
        assertTrue(result.isEmpty());
        Mockito.verifyNoInteractions(webClient);
    }

    @Test
    void testGetServiceKeysByManagedServicesMakesHttpCall() {
        stubWebClientToReturnEmptyPage();

        UUID serviceGuid = UUID.randomUUID();
        DeployedMtaService managedService = buildManagedService("managed-svc", serviceGuid);

        client.getServiceKeysByMetadataAndManagedServices(SPACE_GUID, MTA_ID, MTA_NAMESPACE,
                                                          List.of(managedService));
        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(requestHeadersUriSpec)
               .uri(uriCaptor.capture());

        String capturedUri = uriCaptor.getValue();
        assertTrue(capturedUri.contains("&service_instance_guids=" + serviceGuid), "URI should contain the guid of the managed service");
        Mockito.verify(webClient, Mockito.times(1))
               .get();
    }

    @Test
    void testGetServiceKeysByManagedServicesUsesOnlyManagedGuids() {
        stubWebClientToReturnEmptyPage();

        UUID managedGuid = UUID.randomUUID();
        UUID userProvidedGuid = UUID.randomUUID();
        DeployedMtaService managedService = buildManagedService("managed-svc", managedGuid);
        DeployedMtaService userProvidedService = ImmutableDeployedMtaService.builder()
                                                                            .name("ups")
                                                                            .type(ServiceInstanceType.USER_PROVIDED)
                                                                            .metadata(ImmutableCloudMetadata.builder()
                                                                                                            .guid(userProvidedGuid)
                                                                                                            .build())
                                                                            .build();

        client.getServiceKeysByMetadataAndManagedServices(SPACE_GUID, MTA_ID, MTA_NAMESPACE, List.of(managedService, userProvidedService));

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(requestHeadersUriSpec)
               .uri(uriCaptor.capture());

        String capturedUri = uriCaptor.getValue();
        assertTrue(capturedUri.endsWith("&service_instance_guids=" + managedGuid),
                   "URI should contain only the managed service guid");
        assertFalse(capturedUri.contains(userProvidedGuid.toString()), "URI should not contain the user-provided service guid");
    }

    @Test
    void testGetServiceKeysByManagedServicesReturnsServiceKeys() {
        UUID serviceInstanceGuid = UUID.randomUUID();
        UUID serviceKeyGuid = UUID.randomUUID();

        String serviceKeyName = "sk-1";
        String serviceInstanceName = "svc-1";
        String responseJson = buildServiceKeysResponse(serviceKeyGuid, serviceInstanceGuid, serviceKeyName, serviceInstanceName);
        stubWebClientToReturn(responseJson);

        DeployedMtaService managedService = buildManagedService(serviceInstanceName, serviceInstanceGuid);

        List<DeployedMtaServiceKey> result = client.getServiceKeysByMetadataAndManagedServices(SPACE_GUID, MTA_ID, MTA_NAMESPACE,
                                                                                               List.of(managedService));

        assertEquals(1, result.size());
        assertEquals(serviceKeyName, result.getFirst()
                                           .getName());
        assertEquals(serviceInstanceName, result.getFirst()
                                                .getServiceInstance()
                                                .getName());
    }

    @Test
    void testLabelSelectorContainsSpaceGuidMtaIdAndMtaNamespace() {
        stubWebClientToReturnEmptyPage();

        String guid = UUID.randomUUID()
                          .toString();
        client.getServiceKeysByMetadataAndExistingGuids(SPACE_GUID, MTA_ID, MTA_NAMESPACE, List.of(guid));

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(requestHeadersUriSpec)
               .uri(uriCaptor.capture());

        String capturedUri = uriCaptor.getValue();
        assertTrue(capturedUri.contains("space_guid=" + SPACE_GUID));
        assertTrue(capturedUri.contains("mta_id="));
        assertTrue(capturedUri.contains("mta_namespace="));
    }

    @Test
    void testLabelSelectorWithNullNamespaceUsesDoesNotExist() {
        stubWebClientToReturnEmptyPage();

        String guid = UUID.randomUUID()
                          .toString();
        client.getServiceKeysByMetadataAndExistingGuids(SPACE_GUID, MTA_ID, null, List.of(guid));

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(requestHeadersUriSpec)
               .uri(uriCaptor.capture());

        String capturedUri = uriCaptor.getValue();
        // When namespace is null/empty, the label selector uses "!" prefix (doesNotExist)
        assertTrue(capturedUri.contains("!mta_namespace"), "When namespace is null, label selector should use !mta_namespace");
    }

    @Test
    void testLabelSelectorWithEmptyNamespaceUsesDoesNotExist() {
        stubWebClientToReturnEmptyPage();

        String guid = UUID.randomUUID()
                          .toString();
        client.getServiceKeysByMetadataAndExistingGuids(SPACE_GUID, MTA_ID, "", List.of(guid));

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(requestHeadersUriSpec)
               .uri(uriCaptor.capture());

        String capturedUri = uriCaptor.getValue();
        assertTrue(capturedUri.contains("!mta_namespace"), "When namespace is empty, label selector should use !mta_namespace");
    }

    @Test
    void testBatchingTriggeredWithManyGuids() {
        stubWebClientToReturnEmptyPage();

        // Generate enough GUIDs to force multiple batches (each UUID is 36 chars, limit is 4000)
        List<String> manyGuids = generateRandomGuids(200);

        client.getServiceKeysByMetadataAndExistingGuids(SPACE_GUID, MTA_ID, MTA_NAMESPACE, manyGuids);

        // With 200 GUIDs and a fixed prefix consuming some of the 4000-char budget, multiple requests are expected
        Mockito.verify(webClient, Mockito.atLeast(2))
               .get();
    }

    @Test
    void testSingleBatchWithFewGuids() {
        stubWebClientToReturnEmptyPage();

        List<String> fewGuids = generateRandomGuids(3);

        client.getServiceKeysByMetadataAndExistingGuids(SPACE_GUID, MTA_ID, MTA_NAMESPACE, fewGuids);

        Mockito.verify(webClient, Mockito.times(1))
               .get();
    }

    @Test
    void testBatchedUrisNeverExceedMaxLength() {
        stubWebClientToReturnEmptyPage();

        List<String> manyGuids = generateRandomGuids(800);
        client.getServiceKeysByMetadataAndExistingGuids(SPACE_GUID, MTA_ID, MTA_NAMESPACE, manyGuids);

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
        client.getServiceKeysByMetadataAndExistingGuids(SPACE_GUID, MTA_ID, MTA_NAMESPACE, manyGuids);

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(requestHeadersUriSpec, Mockito.atLeastOnce())
               .uri(uriCaptor.capture());

        List<String> allCapturedGuids = new ArrayList<>();
        for (String uri : uriCaptor.getAllValues()) {
            int idx = uri.indexOf("&service_instance_guids=");
            assertTrue(idx >= 0, "URI should contain &service_instance_guids=");
            String guidsStr = uri.substring(idx + "&service_instance_guids=".length());
            String[] guids = guidsStr.split(",");
            Collections.addAll(allCapturedGuids, guids);
        }

        assertEquals(manyGuids.size(), allCapturedGuids.size(), "All guids must be sent across batched requests");
        assertEquals(manyGuids, allCapturedGuids, "Guids must appear in original order across batches");
    }

    @Test
    void testPaginatedResponseFollowsAllPages() {
        UUID siGuid = UUID.randomUUID();

        UUID keyGuid1 = UUID.randomUUID();
        UUID keyGuid2 = UUID.randomUUID();

        String page1Json = buildServiceKeysResponseWithPagination(keyGuid1, siGuid, "key-1", "svc-1",
                                                                  "/v3/service_credential_bindings?page=2");
        String page2Json = buildServiceKeysResponse(keyGuid2, siGuid, "key-2", "svc-1");

        stubWebClientToReturnSequentially(page1Json, page2Json);

        List<DeployedMtaServiceKey> result = client.getServiceKeysByMetadataAndExistingGuids(SPACE_GUID, MTA_ID, MTA_NAMESPACE,
                                                                                             List.of(siGuid.toString()));

        assertEquals(2, result.size());
        assertEquals("key-1", result.getFirst()
                                    .getName());
        assertEquals("key-2", result.get(1)
                                    .getName());
    }

    @Test
    void testMultipleKeysForSameServiceAreMappedCorrectly() {
        UUID siGuid = UUID.randomUUID();
        UUID keyGuid1 = UUID.randomUUID();
        UUID keyGuid2 = UUID.randomUUID();

        String responseJson = buildMultiKeyResponse(List.of(keyGuid1, keyGuid2), siGuid, List.of("key-a", "key-b"), "shared-service");
        stubWebClientToReturn(responseJson);

        List<DeployedMtaServiceKey> result = client.getServiceKeysByMetadataAndExistingGuids(SPACE_GUID, MTA_ID, MTA_NAMESPACE,
                                                                                             List.of(siGuid.toString()));

        assertEquals(2, result.size());
        assertEquals("key-a", result.getFirst()
                                    .getName());
        assertEquals("key-b", result.get(1)
                                    .getName());
        // Both keys should reference the same service instance
        assertEquals("shared-service", result.getFirst()
                                             .getServiceInstance()
                                             .getName());
        assertEquals("shared-service", result.get(1)
                                             .getServiceInstance()
                                             .getName());
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

    private DeployedMtaService buildManagedService(String name, UUID guid) {
        return ImmutableDeployedMtaService.builder()
                                          .name(name)
                                          .type(ServiceInstanceType.MANAGED)
                                          .metadata(ImmutableCloudMetadata.builder()
                                                                          .guid(guid)
                                                                          .build())
                                          .build();
    }

    private List<String> generateRandomGuids(int count) {
        List<String> guids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            guids.add(UUID.randomUUID()
                          .toString());
        }
        return guids;
    }

    private String buildServiceKeysResponse(UUID serviceKeyGuid, UUID serviceInstanceGuid, String keyName, String serviceName) {
        return buildServiceKeysResponseWithPagination(serviceKeyGuid, serviceInstanceGuid, keyName, serviceName, null);
    }

    private String buildServiceKeysResponseWithPagination(UUID serviceKeyGuid, UUID serviceInstanceGuid, String keyName, String serviceName,
                                                          String nextPageHref) {
        String nextPage = nextPageHref == null ? "null" : "{\"href\":\"" + nextPageHref + "\"}";
        return "{" + "\"resources\":[" + "  {" + "    \"guid\":\"" + serviceKeyGuid + "\"," + "    \"name\":\"" + keyName + "\","
            + "    \"type\":\"key\"," + "    \"created_at\":\"2024-01-01T00:00:00Z\"," + "    \"updated_at\":\"2024-01-01T00:00:00Z\","
            + "    \"metadata\":{\"labels\":{},\"annotations\":{}}," + "    \"relationships\":{"
            + "      \"service_instance\":{\"data\":{\"guid\":\"" + serviceInstanceGuid + "\"}}" + "    }" + "  }" + "]," + "\"included\":{"
            + "  \"service_instances\":[" + "    {" + "      \"guid\":\"" + serviceInstanceGuid + "\"," + "      \"name\":\"" + serviceName
            + "\"," + "      \"type\":\"managed\"," + "      \"created_at\":\"2024-01-01T00:00:00Z\","
            + "      \"updated_at\":\"2024-01-01T00:00:00Z\"," + "      \"metadata\":{\"labels\":{},\"annotations\":{}}" + "    }" + "  ]"
            + "}," + "\"pagination\":{\"next\":" + nextPage + "}" + "}";
    }

    private String buildMultiKeyResponse(List<UUID> keyGuids, UUID serviceInstanceGuid, List<String> keyNames, String serviceName) {
        StringBuilder resources = new StringBuilder();
        for (int i = 0; i < keyGuids.size(); i++) {
            if (i > 0) {
                resources.append(",");
            }
            resources.append("{")
                     .append("\"guid\":\"")
                     .append(keyGuids.get(i))
                     .append("\",")
                     .append("\"name\":\"")
                     .append(keyNames.get(i))
                     .append("\",")
                     .append("\"type\":\"key\",")
                     .append("\"created_at\":\"2024-01-01T00:00:00Z\",")
                     .append("\"updated_at\":\"2024-01-01T00:00:00Z\",")
                     .append("\"metadata\":{\"labels\":{},\"annotations\":{}},")
                     .append("\"relationships\":{")
                     .append("  \"service_instance\":{\"data\":{\"guid\":\"")
                     .append(serviceInstanceGuid)
                     .append("\"}}")
                     .append("}")
                     .append("}");
        }

        return "{" + "\"resources\":[" + resources + "]," + "\"included\":{" + "  \"service_instances\":[" + "    {" + "      \"guid\":\""
            + serviceInstanceGuid + "\"," + "      \"name\":\"" + serviceName + "\"," + "      \"type\":\"managed\","
            + "      \"created_at\":\"2024-01-01T00:00:00Z\"," + "      \"updated_at\":\"2024-01-01T00:00:00Z\","
            + "      \"metadata\":{\"labels\":{},\"annotations\":{}}" + "    }" + "  ]" + "}," + "\"pagination\":{\"next\":null}" + "}";
    }
}

