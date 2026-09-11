package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudTask;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudTask;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;

class TasksV3OperationsTest {

    private static final String APP_GUID = "3725a721-6e3f-4b6e-8f2b-1c2d3e4f5a6b";
    private static final String TASK_GUID = "99999999-8888-7777-6666-555555555555";

    @Mock
    private CloudSpace target;

    private MockControllerClientFactory factory;
    private TasksV3Operations operations;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        factory = MockControllerClientFactory.create();
        operations = new TasksV3Operations(factory.client(), target);
    }

    @Test
    void testGetTaskReturnsMappedTask() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/tasks/" + TASK_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(getTaskJson("SUCCEEDED"), MediaType.APPLICATION_JSON));

        CloudTask result = operations.getTask(UUID.fromString(TASK_GUID));

        Assertions.assertEquals("migrate", result.getName());
        Assertions.assertEquals(CloudTask.State.SUCCEEDED, result.getState());
        factory.verify();
    }

    @Test
    void testRunTaskPostsBodyAndReturnsCreatedTask() {
        stubAppLookup();
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/tasks"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withSuccess(getTaskJson("RUNNING"), MediaType.APPLICATION_JSON));

        CloudTask task = ImmutableCloudTask.builder()
                                           .name("migrate")
                                           .command("rails db:migrate")
                                           .build();

        CloudTask result = operations.runTask("my-app", task);

        Assertions.assertEquals(CloudTask.State.RUNNING, result.getState());
        factory.verify();
    }

    @Test
    void testCancelTaskPostsToCancelAction() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/tasks/" + TASK_GUID
                                                             + "/actions/cancel"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withSuccess(getTaskJson("CANCELING"), MediaType.APPLICATION_JSON));

        CloudTask result = operations.cancelTask(UUID.fromString(TASK_GUID));

        Assertions.assertEquals(CloudTask.State.CANCELING, result.getState());
        factory.verify();
    }

    @Test
    void testRunTaskThrowsWhenApplicationNotFound() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps?per_page=5000&names=missing-app"))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[]}", MediaType.APPLICATION_JSON));

        CloudTask task = ImmutableCloudTask.builder()
                                           .name("migrate")
                                           .command("cmd")
                                           .build();

        Assertions.assertThrows(CloudOperationException.class, () -> operations.runTask("missing-app", task));
    }

    private void stubAppLookup() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps?per_page=5000&names=my-app"))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[{\"guid\":\"" + APP_GUID + "\"}]}",
                                                                MediaType.APPLICATION_JSON));
    }

    private static String getTaskJson(String state) {
        return "{\"guid\":\"" + TASK_GUID + "\",\"name\":\"migrate\",\"command\":\"rails db:migrate\",\"state\":\"" + state
            + "\",\"memory_in_mb\":256,\"disk_in_mb\":1024}";
    }

}
