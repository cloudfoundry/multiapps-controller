package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Job;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;

class CloudControllerV3ClientTest {

    private static final String JOB_GUID = "66666666-6666-6666-6666-666666666666";
    private static final Duration TIMEOUT = Duration.ofMillis(10);

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
