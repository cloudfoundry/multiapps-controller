package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudBuild;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;

class BuildsV3OperationsTest {

    private static final String BUILD_GUID = "11111111-2222-3333-4444-555555555555";
    private static final String PACKAGE_GUID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String APP_GUID = "3725a721-6e3f-4b6e-8f2b-1c2d3e4f5a6b";
    private static final String DROPLET_GUID = "99999999-8888-7777-6666-555555555555";

    private MockControllerClientFactory factory;
    private BuildsV3Operations operations;

    @BeforeEach
    void setUp() {
        factory = MockControllerClientFactory.create();
        operations = new BuildsV3Operations(factory.client());
    }

    @Test
    void testCreateBuildPostsPackageAndReturnsMappedBuild() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/builds"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withSuccess(getBuildJson("STAGING"), MediaType.APPLICATION_JSON));

        CloudBuild result = operations.createBuild(UUID.fromString(PACKAGE_GUID));

        Assertions.assertEquals(CloudBuild.State.STAGING, result.getState());
        Assertions.assertEquals(UUID.fromString(BUILD_GUID), result.getMetadata()
                                                                   .getGuid());
        Assertions.assertEquals(UUID.fromString(PACKAGE_GUID), result.getPackageInfo()
                                                                     .getGuid());
        factory.verify();
    }

    @Test
    void testGetBuildReturnsMappedBuild() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/builds/" + BUILD_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(getBuildJson("STAGED"), MediaType.APPLICATION_JSON));

        CloudBuild result = operations.getBuild(UUID.fromString(BUILD_GUID));

        Assertions.assertEquals(CloudBuild.State.STAGED, result.getState());
        Assertions.assertEquals(UUID.fromString(DROPLET_GUID), result.getDropletInfo()
                                                                     .getGuid());
        factory.verify();
    }

    @Test
    void testGetBuildThrowsWhenNotFound() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/builds/" + BUILD_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withStatus(HttpStatus.NOT_FOUND));

        Assertions.assertThrows(CloudOperationException.class, () -> operations.getBuild(UUID.fromString(BUILD_GUID)));
    }

    @Test
    void testGetBuildsForApplicationReturnsMappedList() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID
                                                             + "/builds?per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[" + getBuildJson("STAGED") + "]}",
                                                                MediaType.APPLICATION_JSON));

        List<CloudBuild> result = operations.getBuildsForApplication(UUID.fromString(APP_GUID));

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(CloudBuild.State.STAGED, result.getFirst()
                                                               .getState());
        factory.verify();
    }

    @Test
    void testGetBuildsForApplicationReturnsEmptyList() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID
                                                             + "/builds?per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[]}", MediaType.APPLICATION_JSON));

        List<CloudBuild> result = operations.getBuildsForApplication(UUID.fromString(APP_GUID));

        Assertions.assertEquals(0, result.size());
        factory.verify();
    }

    @Test
    void testGetBuildsForPackageReturnsMappedList() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/builds?package_guids="
                                                             + PACKAGE_GUID + "&per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[" + getBuildJson("FAILED") + "]}",
                                                                MediaType.APPLICATION_JSON));

        List<CloudBuild> result = operations.getBuildsForPackage(UUID.fromString(PACKAGE_GUID));

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(CloudBuild.State.FAILED, result.getFirst()
                                                               .getState());
        Assertions.assertEquals("staging failed", result.getFirst()
                                                        .getError());
        factory.verify();
    }

    @Test
    void testGetBuildsForPackageReturnsEmptyList() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/builds?package_guids="
                                                             + PACKAGE_GUID + "&per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[]}", MediaType.APPLICATION_JSON));

        List<CloudBuild> result = operations.getBuildsForPackage(UUID.fromString(PACKAGE_GUID));

        Assertions.assertEquals(0, result.size());
        factory.verify();
    }

    private static String getBuildJson(String state) {
        return "{\"guid\":\"" + BUILD_GUID + "\",\"state\":\"" + state + "\",\"error\":\"staging failed\",\"package\":{\"guid\":\""
            + PACKAGE_GUID + "\"},\"droplet\":{\"guid\":\"" + DROPLET_GUID + "\"}}";
    }

}
