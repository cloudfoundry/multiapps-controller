package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceInstanceType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;

class ServiceKeysV3OperationsTest {

    private static final String SERVICE_INSTANCE_GUID = "11111111-2222-3333-4444-555555555555";
    private static final String KEY_GUID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String JOB_GUID = "99999999-8888-7777-6666-555555555555";
    private static final String KEY_NAME = "my-key";

    private static final String LIST_KEYS_QUERY = "/v3/service_credential_bindings?per_page=5000&type=key&service_instance_guids="
        + SERVICE_INSTANCE_GUID;
    private static final String LOOKUP_KEY_QUERY = LIST_KEYS_QUERY + "&names=" + KEY_NAME;

    private MockControllerClientFactory factory;
    private ServiceKeysV3Operations operations;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        factory = MockControllerClientFactory.create();
        operations = new ServiceKeysV3Operations(factory.client());
    }

    @Test
    void testGetServiceKeyReturnsMappedKeyWithCredentials() {
        stubKeyLookup(getServiceKeyResourcesJson());
        stubCredentials();

        CloudServiceKey result = operations.getServiceKey(getManagedServiceInstance(), KEY_NAME);

        Assertions.assertEquals(KEY_NAME, result.getName());
        Assertions.assertEquals("secret", result.getCredentials()
                                                .get("password"));
        Assertions.assertEquals(UUID.fromString(KEY_GUID), result.getMetadata()
                                                                 .getGuid());
        factory.verify();
    }

    @Test
    void testGetServiceKeyReturnsNullWhenNotFound() {
        stubKeyLookup("{\"resources\":[]}");

        CloudServiceKey result = operations.getServiceKey(getManagedServiceInstance(), KEY_NAME);

        Assertions.assertNull(result);
        factory.verify();
    }

    @Test
    void testGetServiceKeysReturnsMappedKeysWithoutCredentials() {
        stubKeyList(getServiceKeyResourcesJson());

        List<CloudServiceKey> result = operations.getServiceKeys(getManagedServiceInstance());

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(KEY_NAME, result.getFirst()
                                                .getName());
        factory.verify();
    }

    @Test
    void testGetServiceKeysReturnsEmptyListWhenNoKeys() {
        stubKeyList("{\"resources\":[]}");

        List<CloudServiceKey> result = operations.getServiceKeys(getManagedServiceInstance());

        Assertions.assertEquals(0, result.size());
        factory.verify();
    }

    @Test
    void testGetServiceKeysWithCredentialsFetchesDetailsPerKey() {
        stubKeyList(getServiceKeyResourcesJson());
        stubCredentials();

        List<CloudServiceKey> result = operations.getServiceKeysWithCredentials(getManagedServiceInstance());

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("secret", result.getFirst()
                                                .getCredentials()
                                                .get("password"));
        factory.verify();
    }

    @Test
    void testCreateServiceKeyFromModelReturnsJobGuid() {
        stubAcceptedPost();

        Optional<String> result = operations.createServiceKey(getServiceKeyModel(), getManagedServiceInstance());

        Assertions.assertEquals(Optional.of(JOB_GUID), result);
        factory.verify();
    }

    @Test
    void testCreateServiceKeyWithParametersReturnsJobGuid() {
        stubAcceptedPost();

        Optional<String> result = operations.createServiceKey(getManagedServiceInstance(), KEY_NAME, Map.of("k", "v"));

        Assertions.assertEquals(Optional.of(JOB_GUID), result);
        factory.verify();
    }

    @Test
    void testCreateServiceKeyReturnsEmptyWhenNoLocationHeader() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(
                   MockControllerClientFactory.BASE_URL + "/v3/service_credential_bindings"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withStatus(HttpStatus.ACCEPTED));

        Optional<String> result = operations.createServiceKey(getManagedServiceInstance(), KEY_NAME, null);

        Assertions.assertEquals(Optional.empty(), result);
        factory.verify();
    }

    @Test
    void testCreateServiceKeyThrowsForUserProvidedService() {
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> operations.createServiceKey(getUserProvidedServiceInstance(), KEY_NAME, null));
    }

    @Test
    void testCreateAndFetchServiceKeyFollowsJobThenReturnsMappedKey() {
        stubAcceptedPost();
        stubJobComplete();
        stubKeyLookup(getServiceKeyResourcesJson());
        stubCredentials();

        CloudServiceKey result = operations.createAndFetchServiceKey(getServiceKeyModel(), getManagedServiceInstance());

        Assertions.assertEquals(KEY_NAME, result.getName());
        Assertions.assertEquals("secret", result.getCredentials()
                                                .get("password"));
        factory.verify();
    }

    @Test
    void testCreateAndFetchServiceKeyReturnsNullWhenKeyNotFoundAfterJob() {
        stubAcceptedPost();
        stubJobComplete();
        stubKeyLookup("{\"resources\":[]}");

        CloudServiceKey result = operations.createAndFetchServiceKey(getServiceKeyModel(), getManagedServiceInstance());

        Assertions.assertNull(result);
        factory.verify();
    }

    private void stubKeyLookup(String responseJson) {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + LOOKUP_KEY_QUERY))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(responseJson, MediaType.APPLICATION_JSON));
    }

    private void stubKeyList(String responseJson) {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + LIST_KEYS_QUERY))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(responseJson, MediaType.APPLICATION_JSON));
    }

    private void stubCredentials() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(
                   MockControllerClientFactory.BASE_URL + "/v3/service_credential_bindings/" + KEY_GUID + "/details"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"credentials\":{\"password\":\"secret\"}}",
                                                                MediaType.APPLICATION_JSON));
    }

    private void stubAcceptedPost() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(
                   MockControllerClientFactory.BASE_URL + "/v3/service_credential_bindings"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withStatus(HttpStatus.ACCEPTED)
                                                   .location(java.net.URI.create(
                                                       MockControllerClientFactory.BASE_URL + "/v3/jobs/" + JOB_GUID)));
    }

    private void stubJobComplete() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/jobs/" + JOB_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"guid\":\"" + JOB_GUID + "\",\"state\":\"COMPLETE\"}",
                                                                MediaType.APPLICATION_JSON));
    }

    private static String getServiceKeyResourcesJson() {
        return "{\"resources\":[{\"guid\":\"" + KEY_GUID + "\",\"name\":\"" + KEY_NAME + "\",\"type\":\"key\"}]}";
    }

    private static CloudServiceInstance getManagedServiceInstance() {
        return ImmutableCloudServiceInstance.builder()
                                            .name("my-service")
                                            .metadata(ImmutableCloudMetadata.of(UUID.fromString(SERVICE_INSTANCE_GUID)))
                                            .type(ServiceInstanceType.MANAGED)
                                            .build();
    }

    private static CloudServiceInstance getUserProvidedServiceInstance() {
        return ImmutableCloudServiceInstance.builder()
                                            .name("my-ups")
                                            .metadata(ImmutableCloudMetadata.of(UUID.fromString(SERVICE_INSTANCE_GUID)))
                                            .type(ServiceInstanceType.USER_PROVIDED)
                                            .build();
    }

    private static CloudServiceKey getServiceKeyModel() {
        return ImmutableCloudServiceKey.builder()
                                       .name(KEY_NAME)
                                       .build();
    }

}
