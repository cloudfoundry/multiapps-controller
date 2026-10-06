package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudPackage;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.DockerInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableDockerCredentials;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableDockerInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Status;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Upload;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;

class PackagesV3OperationsTest {

    private static final String PACKAGE_GUID = "11111111-2222-3333-4444-555555555555";
    private static final String APP_GUID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

    @Mock
    private CloudSpace target;

    private MockControllerClientFactory factory;
    private PackagesV3Operations operations;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        factory = MockControllerClientFactory.create();
        operations = new PackagesV3Operations(factory.client(), target);
    }

    @Test
    void testGetPackageReturnsMappedPackage() {
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/packages/" + PACKAGE_GUID, bitsPackageJson("READY", null));

        CloudPackage result = operations.getPackage(UUID.fromString(PACKAGE_GUID));

        Assertions.assertEquals(CloudPackage.Type.BITS, result.getType());
        Assertions.assertEquals(Status.READY, result.getStatus());
        Assertions.assertEquals(PACKAGE_GUID, result.getGuid()
                                                    .toString());
        factory.verify();
    }

    @Test
    void testGetPackageThrowsWhenResponseIsNull() {
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/packages/" + PACKAGE_GUID, "");

        UUID packageGuid = UUID.fromString(PACKAGE_GUID);
        Assertions.assertThrows(CloudOperationException.class, () -> operations.getPackage(packageGuid));
    }

    @Test
    void testGetPackageThrowsOnNotFoundStatus() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/packages/" + PACKAGE_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withStatus(HttpStatus.NOT_FOUND));

        UUID packageGuid = UUID.fromString(PACKAGE_GUID);
        Assertions.assertThrows(CloudOperationException.class, () -> operations.getPackage(packageGuid));
    }

    @Test
    void testGetPackagesForApplicationReturnsMappedList() {
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/packages?per_page=5000",
                        "{\"resources\":[" + bitsPackageJson("READY", null) + "]}");

        List<CloudPackage> result = operations.getPackagesForApplication(UUID.fromString(APP_GUID));

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(Status.READY, result.getFirst()
                                                    .getStatus());
        factory.verify();
    }

    @Test
    void testGetPackagesForApplicationReturnsEmptyList() {
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/packages?per_page=5000",
                        "{\"resources\":[]}");

        List<CloudPackage> result = operations.getPackagesForApplication(UUID.fromString(APP_GUID));

        Assertions.assertEquals(0, result.size());
        factory.verify();
    }

    @Test
    void testCreateDockerPackagePostsAndReturnsCreatedPackage() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/packages"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withSuccess(dockerPackageJson(), MediaType.APPLICATION_JSON));

        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/packages/" + PACKAGE_GUID, dockerPackageJson());

        DockerInfo dockerInfo = ImmutableDockerInfo.builder()
                                                   .image("my/image:latest")
                                                   .build();

        CloudPackage result = operations.createDockerPackage(UUID.fromString(APP_GUID), dockerInfo);

        Assertions.assertEquals(CloudPackage.Type.DOCKER, result.getType());
        Assertions.assertEquals(PACKAGE_GUID, result.getGuid()
                                                    .toString());
        factory.verify();
    }

    @Test
    void testCreateDockerPackageWithCredentials() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/packages"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withSuccess(dockerPackageJson(), MediaType.APPLICATION_JSON));

        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/packages/" + PACKAGE_GUID, dockerPackageJson());

        DockerInfo dockerInfo = ImmutableDockerInfo.builder()
                                                   .image("my/image:latest")
                                                   .credentials(ImmutableDockerCredentials.builder()
                                                                                          .username("user")
                                                                                          .password("secret")
                                                                                          .build())
                                                   .build();

        CloudPackage result = operations.createDockerPackage(UUID.fromString(APP_GUID), dockerInfo);

        Assertions.assertEquals(CloudPackage.Type.DOCKER, result.getType());
        factory.verify();
    }

    @Test
    void testGetUploadStatusForBitsPackageIncludesErrorDetails() {
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/packages/" + PACKAGE_GUID, bitsPackageJson("FAILED", "boom"));

        Upload result = operations.getUploadStatus(UUID.fromString(PACKAGE_GUID));

        Assertions.assertEquals(Status.FAILED, result.getStatus());
        Assertions.assertEquals("boom", result.getErrorDetails()
                                              .getDescription());
        factory.verify();
    }

    @Test
    void testGetUploadStatusForDockerPackageHasNoErrorDetails() {
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/packages/" + PACKAGE_GUID, dockerPackageJson());

        Upload result = operations.getUploadStatus(UUID.fromString(PACKAGE_GUID));

        Assertions.assertEquals(Status.READY, result.getStatus());
        Assertions.assertNull(result.getErrorDetails());
        factory.verify();
    }

    private static String bitsPackageJson(String state, String error) {
        String data = error == null ? "{}" : "{\"error\":\"" + error + "\"}";
        return "{\"guid\":\"" + PACKAGE_GUID + "\",\"type\":\"bits\",\"state\":\"" + state + "\",\"data\":" + data + "}";
    }

    private static String dockerPackageJson() {
        return "{\"guid\":\"" + PACKAGE_GUID
            + "\",\"type\":\"docker\",\"state\":\"READY\",\"data\":{\"image\":\"my/image:latest\"}}";
    }

}
