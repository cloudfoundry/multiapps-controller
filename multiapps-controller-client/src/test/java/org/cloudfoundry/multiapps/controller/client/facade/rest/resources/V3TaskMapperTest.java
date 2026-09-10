package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudTask;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class V3TaskMapperTest {

    private static final String GUID_STRING = "3725a721-6e3f-4b6e-8f2b-1c2d3e4f5a6b";

    @Test
    void testToCloudTaskMapsAllFields() {
        V3Task task = new V3Task(GUID_STRING, "migrate-db", "rails db:migrate", "SUCCEEDED", 256, 1024,
                                 new V3Task.V3Result(null), "2026-08-04T10:15:30Z", "2026-08-04T10:16:30Z", null);

        CloudTask result = V3TaskMapper.toCloudTask(task);

        Assertions.assertEquals("migrate-db", result.getName());
        Assertions.assertEquals("rails db:migrate", result.getCommand());
        Assertions.assertEquals(CloudTask.State.SUCCEEDED, result.getState());
        Assertions.assertEquals(256, result.getLimits()
                                           .getMemory());
        Assertions.assertEquals(1024, result.getLimits()
                                            .getDisk());
        Assertions.assertNull(result.getResult()
                                    .getFailureReason());
    }

    @Test
    void testToCloudTaskWithNullResultReturnsNullResult() {
        V3Task task = buildTaskWithState("RUNNING");

        Assertions.assertNull(V3TaskMapper.toCloudTask(task)
                                          .getResult());
    }

    @Test
    void testToCloudTaskMapsFailureReason() {
        V3Task task = new V3Task(GUID_STRING, "t", "cmd", "FAILED", 256, 1024, new V3Task.V3Result("boom"), null, null, null);

        Assertions.assertEquals("boom", V3TaskMapper.toCloudTask(task)
                                                    .getResult()
                                                    .getFailureReason());
    }

    @Test
    void testToCloudTaskWithNullStateReturnsNullState() {
        V3Task task = buildTaskWithState(null);

        Assertions.assertNull(V3TaskMapper.toCloudTask(task)
                                          .getState());
    }

    @Test
    void testToCloudTaskWithUnknownStateThrows() {
        V3Task task = buildTaskWithState("bogus");

        Assertions.assertThrows(IllegalArgumentException.class, () -> V3TaskMapper.toCloudTask(task));
    }

    private static V3Task buildTaskWithState(String state) {
        return new V3Task(GUID_STRING, "t", "cmd", state, 256, 1024, null, null, null, null);
    }

}
