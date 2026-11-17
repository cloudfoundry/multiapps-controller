package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Job;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;

class CloudControllerV3ClientTest {

    private static final String JOB_GUID = "66666666-6666-6666-6666-666666666666";
    private static final Duration TIMEOUT = Duration.ofMillis(10);

    private static final ParameterizedTypeReference<V3ListResponse<V3Job>> JOB_PAGE = new ParameterizedTypeReference<>() {
    };

    private MockControllerClientFactory factory;
    private CloudControllerV3Client client;

    @BeforeEach
    void setUp() {
        factory = MockControllerClientFactory.create();
        client = factory.client();
    }

    @Test
    void testGetReturnsMappedBody() {
        stubJobGet(getJobJson("COMPLETE"));

        V3Job job = client.get("/v3/jobs/" + JOB_GUID, V3Job.class);

        Assertions.assertEquals(JOB_GUID, job.guid());
        Assertions.assertEquals("COMPLETE", job.state());
        factory.verify();
    }

    @Test
    void testGetThrowsOnNotFound() {
        stubJobGetStatus(HttpStatus.NOT_FOUND);

        Assertions.assertThrows(CloudOperationException.class, () -> client.get("/v3/jobs/" + JOB_GUID, V3Job.class));
    }

    @Test
    void testGetOptionalReturnsPresentOn200() {
        stubJobGet(getJobJson("COMPLETE"));

        Optional<V3Job> job = client.getOptional("/v3/jobs/" + JOB_GUID, V3Job.class);

        Assertions.assertTrue(job.isPresent());
        Assertions.assertEquals(JOB_GUID, job.get()
                                             .guid());
        factory.verify();
    }

    @Test
    void testGetOptionalReturnsEmptyOn404() {
        stubJobGetStatus(HttpStatus.NOT_FOUND);

        Optional<V3Job> job = client.getOptional("/v3/jobs/" + JOB_GUID, V3Job.class);

        Assertions.assertTrue(job.isEmpty());
        factory.verify();
    }

    @Test
    void testGetOptionalRethrowsNonNotFound() {
        stubJobGetStatus(HttpStatus.INTERNAL_SERVER_ERROR);

        Assertions.assertThrows(CloudOperationException.class, () -> client.getOptional("/v3/jobs/" + JOB_GUID, V3Job.class));
    }

    @Test
    void testWaitForAsyncJobReturnsWhenComplete() {
        stubJobGet(getJobJson("COMPLETE"));

        V3Job job = client.waitForAsyncJob("/" + JOB_GUID, TIMEOUT);

        Assertions.assertEquals(JOB_GUID, job.guid());
        Assertions.assertTrue(job.isComplete());
        factory.verify();
    }

    @Test
    void testWaitForAsyncJobThrowsWhenFailed() {
        stubJobGet(getFailedJobJson());

        CloudOperationException exception = Assertions.assertThrows(CloudOperationException.class,
                                                                    () -> client.waitForAsyncJob("/" + JOB_GUID, TIMEOUT));

        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
        factory.verify();
    }

    @Test
    void testFollowAsyncJobFollowsLocation() {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(MockControllerClientFactory.BASE_URL + "/v3/jobs/" + JOB_GUID));
        ResponseEntity<Void> accepted = new ResponseEntity<>(headers, HttpStatus.ACCEPTED);

        stubJobGet(getJobJson("COMPLETE"));

        client.followAsyncJob(accepted, TIMEOUT);

        factory.verify();
    }

    @Test
    void testFollowAsyncJobReturnsImmediatelyWhenNoLocation() {
        ResponseEntity<Void> created = new ResponseEntity<>(new HttpHeaders(), HttpStatus.CREATED);

        client.followAsyncJob(created, TIMEOUT);

        factory.verify();
    }

    @Test
    void testPostForObjectWithBodySendsBodyAndReturnsMappedResponse() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/jobs"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andExpect(MockRestRequestMatchers.content()
                                                 .string(Matchers.containsString("\"name\":\"value\"")))
               .andRespond(MockRestResponseCreators.withSuccess(getJobJson("PROCESSING"), MediaType.APPLICATION_JSON));

        V3Job result = client.postForObject("/v3/jobs", Map.of("name", "value"), V3Job.class);

        Assertions.assertEquals(JOB_GUID, result.guid());
        factory.verify();
    }

    @Test
    void testPostForObjectWithNullBodySendsNoBody() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/jobs"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andExpect(MockRestRequestMatchers.content()
                                                 .string(""))
               .andRespond(MockRestResponseCreators.withSuccess(getJobJson("PROCESSING"), MediaType.APPLICATION_JSON));

        V3Job result = client.postForObject("/v3/jobs", null, V3Job.class);

        Assertions.assertEquals(JOB_GUID, result.guid());
        factory.verify();
    }

    @Test
    void testPostWithBodyReturnsBodilessEntity() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/guid/actions/stop"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andExpect(MockRestRequestMatchers.content()
                                                 .string(Matchers.containsString("\"k\":\"v\"")))
               .andRespond(MockRestResponseCreators.withSuccess());

        ResponseEntity<Void> result = client.post("/v3/apps/{guid}/actions/stop", Map.of("k", "v"), "guid");

        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        factory.verify();
    }

    @Test
    void testPostWithNullBodySendsNoBody() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/guid/actions/start"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andExpect(MockRestRequestMatchers.content()
                                                 .string(""))
               .andRespond(MockRestResponseCreators.withSuccess());

        client.post("/v3/apps/{guid}/actions/start", null, "guid");

        factory.verify();
    }

    @Test
    void testPatchSendsBodyToUri() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/guid"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.PATCH))
               .andExpect(MockRestRequestMatchers.content()
                                                 .string(Matchers.containsString("\"name\":\"new\"")))
               .andRespond(MockRestResponseCreators.withSuccess());

        client.patch("/v3/apps/{guid}", Map.of("name", "new"), "guid");

        factory.verify();
    }

    @Test
    void testDeleteSendsDeleteToUri() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/guid"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.DELETE))
               .andRespond(MockRestResponseCreators.withSuccess());

        client.delete("/v3/apps/{guid}", "guid");

        factory.verify();
    }

    @Test
    void testListReturnsEmptyWhenUriIsNull() {
        Assertions.assertTrue(client.list(null, JOB_PAGE)
                                    .isEmpty());
    }

    @Test
    void testListReturnsEmptyWhenFirstPageHasNoBody() {
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/jobs", null);

        Assertions.assertTrue(client.list(MockControllerClientFactory.BASE_URL + "/v3/jobs", JOB_PAGE)
                                    .isEmpty());
        factory.verify();
    }

    @Test
    void testListReturnsSinglePageWhenNoPagination() {
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/jobs", "{\"resources\":[" + getJobJson("COMPLETE") + "]}");

        java.util.List<V3Job> result = client.list(MockControllerClientFactory.BASE_URL + "/v3/jobs", JOB_PAGE);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(JOB_GUID, result.getFirst()
                                                .guid());
        factory.verify();
    }

    @Test
    void testListFollowsNextPageHrefSequentiallyUntilExhausted() {
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/jobs",
                        "{\"pagination\":{\"next\":{\"href\":\"" + MockControllerClientFactory.BASE_URL
                            + "/v3/jobs?page=2&per_page=50\"}},\"resources\":[" + getJobJson("COMPLETE") + "]}");

        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/jobs?page=2&per_page=50",
                        "{\"pagination\":{\"next\":null},\"resources\":[" + getJobJson("PROCESSING") + "]}");

        List<V3Job> result = client.list(MockControllerClientFactory.BASE_URL + "/v3/jobs", JOB_PAGE);

        Assertions.assertEquals(2, result.size());
        factory.verify();
    }

    @Test
    void testListStopsWhenNextPageHasNoBody() {
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/jobs",
                        "{\"pagination\":{\"next\":{\"href\":\"" + MockControllerClientFactory.BASE_URL
                            + "/v3/jobs?page=2\"}},\"resources\":[" + getJobJson("COMPLETE") + "]}");

        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/jobs?page=2", null);

        List<V3Job> result = client.list(MockControllerClientFactory.BASE_URL + "/v3/jobs", JOB_PAGE);

        Assertions.assertEquals(1, result.size());
        factory.verify();
    }

    @Test
    void testWaitForAsyncJobFailureUsesTitleWhenDetailAbsent() {
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/jobs/" + JOB_GUID,
                        "{\"guid\":\"" + JOB_GUID + "\",\"state\":\"FAILED\",\"operation\":\"service_broker.create\","
                            + "\"errors\":[{\"title\":\"CF-SomeTitle\"}]}");

        CloudOperationException exception = Assertions.assertThrows(CloudOperationException.class,
                                                                    () -> client.waitForAsyncJob("/" + JOB_GUID, TIMEOUT));

        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
        Assertions.assertTrue(exception.getMessage()
                                       .contains("CF-SomeTitle"), exception.getMessage());
        factory.verify();
    }

    @Test
    void testWaitForAsyncJobFailureWithEmptyErrorsUsesGenericDetail() {
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/jobs/" + JOB_GUID,
                        "{\"guid\":\"" + JOB_GUID + "\",\"state\":\"FAILED\",\"operation\":\"service_broker.create\",\"errors\":[]}");

        CloudOperationException exception = Assertions.assertThrows(CloudOperationException.class,
                                                                    () -> client.waitForAsyncJob("/" + JOB_GUID, TIMEOUT));

        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
        factory.verify();
    }

    private void stubJobGet(String jobJson) {
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/jobs/" + JOB_GUID, jobJson);
    }

    private void stubJobGetStatus(HttpStatus status) {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/jobs/" + JOB_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withStatus(status));
    }

    private static String getJobJson(String state) {
        return "{\"guid\":\"" + JOB_GUID + "\",\"state\":\"" + state + "\"}";
    }

    private static String getFailedJobJson() {
        return "{\"guid\":\"" + JOB_GUID + "\",\"state\":\"FAILED\",\"errors\":[{\"detail\":\"boom\",\"title\":\"CF-JobFailed\","
            + "\"code\":10008}]}";
    }

}
