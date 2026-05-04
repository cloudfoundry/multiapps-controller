package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.client.v3.serviceinstances.ServiceInstanceType;
import org.cloudfoundry.multiapps.controller.client.facade.CloudCredentials;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudMetadata;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaService;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaServiceKey;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMtaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomServiceKeysClientTest extends CustomControllerClientBaseTest {

    private static final String SPACE_GUID = "space-guid-123";
    private static final String MTA_ID = "my-mta";
    private static final String MTA_NAMESPACE = "my-namespace";
    private static final String CORRELATION_ID = "test-correlation-id";

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
        stubWebClientToReturnResponse();

        String randomServiceGuid = UUID.randomUUID()
                                       .toString();
        List<String> guidsWithNulls = new ArrayList<>();
        guidsWithNulls.add(null);
        guidsWithNulls.add(randomServiceGuid);
        guidsWithNulls.add(null);

        client.getServiceKeysByMetadataAndExistingGuids(SPACE_GUID, MTA_ID, MTA_NAMESPACE,
                                                        guidsWithNulls);

        String capturedUri = capturedResolvedUris.getFirst();
        assertTrue(capturedUri.endsWith(MessageFormat.format("&service_instance_guids={0}", randomServiceGuid)),
                   "service_instance_guids should contain only the non-null guid");
    }

    @Test
    void testGetServiceKeysByExistingGuidsBuildsCorrectUri() {
        stubWebClientToReturnResponse();

        String guid = UUID.randomUUID()
                          .toString();
        client.getServiceKeysByMetadataAndExistingGuids(SPACE_GUID, MTA_ID, MTA_NAMESPACE, List.of(guid));

        String capturedUri = capturedResolvedUris.getFirst();
        assertTrue(capturedUri.startsWith("/v3/service_credential_bindings?type=key&label_selector="),
                   "URI should start with the service keys base path");
        assertTrue(capturedUri.contains("space_guid=" + SPACE_GUID), "URI should contain the space_guid label selector but was: " + capturedUri);
        assertTrue(capturedUri.contains("&include=service_instance"), "URI should include service_instance but was: " + capturedUri);
        assertTrue(capturedUri.contains("&service_instance_guids=" + guid),
                   "URI should contain the service instance guid parameter but was: " + capturedUri);
    }

    @Test
    void testGetServiceKeysByExistingGuidsJoinsMultipleGuidsWithComma() {
        stubWebClientToReturnResponse();

        String guid1 = UUID.randomUUID()
                           .toString();
        String guid2 = UUID.randomUUID()
                           .toString();
        client.getServiceKeysByMetadataAndExistingGuids(SPACE_GUID, MTA_ID, MTA_NAMESPACE, List.of(guid1, guid2));

        String capturedUri = capturedResolvedUris.getFirst();
        assertTrue(capturedUri.contains("&service_instance_guids=" + guid1 + "," + guid2),
                   "URI should contain both guids joined by comma");
    }

    @Test
    void testGetServiceKeysByExistingGuidsReturnsServiceKeys() {
        UUID serviceInstanceGuid = UUID.randomUUID();
        UUID serviceKeyGuid = UUID.randomUUID();

        String responseJson = buildServiceKeysResponse(serviceKeyGuid, serviceInstanceGuid, "my-key", "my-service", null);
        stubWebClientToReturnResponse(responseJson);

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
        stubWebClientToReturnResponse();

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
        stubWebClientToReturnResponse();

        UUID serviceGuid = UUID.randomUUID();
        DeployedMtaService managedService = buildManagedService("managed-svc", serviceGuid);

        client.getServiceKeysByMetadataAndManagedServices(SPACE_GUID, MTA_ID, MTA_NAMESPACE,
                                                          List.of(managedService));

        String capturedUri = capturedResolvedUris.getFirst();
        assertTrue(capturedUri.contains("&service_instance_guids=" + serviceGuid), "URI should contain the guid of the managed service");
        Mockito.verify(webClient, Mockito.times(1))
               .get();
    }

    @Test
    void testGetServiceKeysByManagedServicesUsesOnlyManagedGuids() {
        stubWebClientToReturnResponse();

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

        String capturedUri = capturedResolvedUris.getFirst();
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
        String responseJson = buildServiceKeysResponse(serviceKeyGuid, serviceInstanceGuid, serviceKeyName, serviceInstanceName, null);
        stubWebClientToReturnResponse(responseJson);

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
        stubWebClientToReturnResponse();

        String guid = UUID.randomUUID()
                          .toString();
        client.getServiceKeysByMetadataAndExistingGuids(SPACE_GUID, MTA_ID, MTA_NAMESPACE, List.of(guid));

        String capturedUri = capturedResolvedUris.getFirst();
        assertTrue(capturedUri.contains("space_guid=" + SPACE_GUID));
        assertTrue(capturedUri.contains("mta_id="));
        assertTrue(capturedUri.contains("mta_namespace="));
    }

    @Test
    void testLabelSelectorWithNullNamespaceUsesDoesNotExist() {
        stubWebClientToReturnResponse();

        String guid = UUID.randomUUID()
                          .toString();
        client.getServiceKeysByMetadataAndExistingGuids(SPACE_GUID, MTA_ID, null, List.of(guid));

        String capturedUri = capturedResolvedUris.getFirst();
        // When namespace is null/empty, the label selector uses "!" prefix (doesNotExist)
        assertTrue(capturedUri.contains("!mta_namespace"), "When namespace is null, label selector should use !mta_namespace");
    }

    @Test
    void testLabelSelectorWithEmptyNamespaceUsesDoesNotExist() {
        stubWebClientToReturnResponse();

        String guid = UUID.randomUUID()
                          .toString();
        client.getServiceKeysByMetadataAndExistingGuids(SPACE_GUID, MTA_ID, "", List.of(guid));

        String capturedUri = capturedResolvedUris.getFirst();
        assertTrue(capturedUri.contains("!mta_namespace"), "When namespace is empty, label selector should use !mta_namespace");
    }

    @Test
    void testBatchingTriggeredWithManyGuids() {
        stubWebClientToReturnResponse();

        // Generate enough GUIDs to force multiple batches (each UUID is 36 chars, limit is 4000)
        List<String> manyGuids = generateRandomGuids(200);

        client.getServiceKeysByMetadataAndExistingGuids(SPACE_GUID, MTA_ID, MTA_NAMESPACE, manyGuids);

        // With 200 GUIDs and a fixed prefix consuming some of the 4000-char budget, multiple requests are expected
        Mockito.verify(webClient, Mockito.atLeast(2))
               .get();
    }

    @Test
    void testSingleBatchWithFewGuids() {
        stubWebClientToReturnResponse();

        List<String> fewGuids = generateRandomGuids(3);

        client.getServiceKeysByMetadataAndExistingGuids(SPACE_GUID, MTA_ID, MTA_NAMESPACE, fewGuids);

        Mockito.verify(webClient, Mockito.times(1))
               .get();
    }

    @Test
    void testBatchedUrisNeverExceedMaxLength() {
        stubWebClientToReturnResponse();

        List<String> manyGuids = generateRandomGuids(800);
        client.getServiceKeysByMetadataAndExistingGuids(SPACE_GUID, MTA_ID, MTA_NAMESPACE, manyGuids);

        for (String uri : capturedResolvedUris) {
            assertTrue(uri.length() <= CustomControllerClient.MAX_URI_QUERY_LENGTH,
                       "URI length " + uri.length() + " exceeds MAX_URI_QUERY_LENGTH "
                           + CustomControllerClient.MAX_URI_QUERY_LENGTH);
        }
    }

    @Test
    void testBatchedRequestsContainAllGuidsInOrder() {
        stubWebClientToReturnResponse();

        List<String> manyGuids = generateRandomGuids(200);
        client.getServiceKeysByMetadataAndExistingGuids(SPACE_GUID, MTA_ID, MTA_NAMESPACE, manyGuids);

        List<String> allCapturedGuids = new ArrayList<>();
        for (String uri : capturedResolvedUris) {
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

        String page1Json = buildServiceKeysResponse(keyGuid1, siGuid, "key-1", "svc-1",
                                                    "/v3/service_credential_bindings?page=2");
        String page2Json = buildServiceKeysResponse(keyGuid2, siGuid, "key-2", "svc-1", null);

        stubWebClientToReturnResponse(page1Json, page2Json);

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
        stubWebClientToReturnResponse(responseJson);

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

    private String buildServiceKeysResponse(UUID serviceKeyGuid, UUID serviceInstanceGuid, String keyName, String serviceName,
                                            String nextPageHref) {
        String keyResourceJson = buildServiceKeyResourceJson(serviceKeyGuid, keyName, serviceInstanceGuid);
        String serviceInstanceJson = buildServiceInstanceJson(serviceInstanceGuid, serviceName);
        return assembleResponseJson(keyResourceJson, serviceInstanceJson, buildPaginationJson(nextPageHref));
    }

    private String buildServiceKeyResourceJson(UUID keyGuid, String keyName, UUID serviceInstanceGuid) {
        return "{\"guid\":\"" + keyGuid + "\","
            + "\"name\":\"" + keyName + "\","
            + "\"type\":\"key\","
            + "\"created_at\":\"2024-01-01T00:00:00Z\","
            + "\"updated_at\":\"2024-01-01T00:00:00Z\","
            + "\"metadata\":{\"labels\":{},\"annotations\":{}},"
            + "\"relationships\":{\"service_instance\":{\"data\":{\"guid\":\"" + serviceInstanceGuid + "\"}}}"
            + "}";
    }

    private String buildServiceInstanceJson(UUID serviceInstanceGuid, String serviceName) {
        return "{\"guid\":\"" + serviceInstanceGuid + "\","
            + "\"name\":\"" + serviceName + "\","
            + "\"type\":\"managed\","
            + "\"created_at\":\"2024-01-01T00:00:00Z\","
            + "\"updated_at\":\"2024-01-01T00:00:00Z\","
            + "\"metadata\":{\"labels\":{},\"annotations\":{}}}";
    }

    private String buildPaginationJson(String nextPageHref) {
        String nextPage = nextPageHref == null ? "null" : "{\"href\":\"" + nextPageHref + "\"}";
        return "{\"next\":" + nextPage + "}";
    }

    private String assembleResponseJson(String resourcesJson, String serviceInstancesJson, String paginationJson) {
        return "{\"resources\":[" + resourcesJson + "],"
            + "\"included\":{\"service_instances\":[" + serviceInstancesJson + "]},"
            + "\"pagination\":" + paginationJson + "}";
    }

    private String buildMultiKeyResponse(List<UUID> keyGuids, UUID serviceInstanceGuid, List<String> keyNames, String serviceName) {
        String keyResourcesJson = buildMultipleServiceKeyResourcesJson(keyGuids, keyNames, serviceInstanceGuid);
        String serviceInstanceJson = buildServiceInstanceJson(serviceInstanceGuid, serviceName);
        return assembleResponseJson(keyResourcesJson, serviceInstanceJson, buildPaginationJson(null));
    }

    private String buildMultipleServiceKeyResourcesJson(List<UUID> keyGuids, List<String> keyNames, UUID serviceInstanceGuid) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keyGuids.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(buildServiceKeyResourceJson(keyGuids.get(i), keyNames.get(i), serviceInstanceGuid));
        }
        return sb.toString();
    }
}

