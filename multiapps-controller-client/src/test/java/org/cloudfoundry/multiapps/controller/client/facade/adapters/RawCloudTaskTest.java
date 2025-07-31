package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import org.cloudfoundry.client.v3.tasks.Result;
import org.cloudfoundry.client.v3.tasks.Task;
import org.cloudfoundry.client.v3.tasks.TaskResource;
import org.cloudfoundry.client.v3.tasks.TaskState;
import org.junit.jupiter.api.Test;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudTask;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudTask;

class RawCloudTaskTest {

    private static final String NAME = "echo";
    private static final String COMMAND = "echo \"Hello!\"";
    private static final int DISK_IN_MB = 128;
    private static final int MEMORY_IN_MB = 256;
    private static final String FAILURE_REASON = "blabla";
    private static final String DROPLET_ID = "8ff24465-823b-4a55-85ee-a680f2c743cd";
    private static final int SEQUENCE_ID = 3;
    private static final TaskState STATE = TaskState.FAILED;

    private static final CloudTask.State EXPECTED_STATE = CloudTask.State.FAILED;

    @Test
    void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedTask(), buildRawTask());
    }

    private static CloudTask buildExpectedTask() {
        return ImmutableCloudTask.builder()
                                 .metadata(RawCloudEntityTest.EXPECTED_METADATA_PARSED_FROM_V3_RESOURCE)
                                 .name(NAME)
                                 .command(COMMAND)
                                 .limits(ImmutableCloudTask.ImmutableLimits.builder()
                                                                           .disk(DISK_IN_MB)
                                                                           .memory(MEMORY_IN_MB)
                                                                           .build())
                                 .result(ImmutableCloudTask.ImmutableResult.builder()
                                                                           .failureReason(FAILURE_REASON)
                                                                           .build())
                                 .state(EXPECTED_STATE)
                                 .build();
    }

    private static RawCloudTask buildRawTask() {
        return ImmutableRawCloudTask.of(buildTestResource());
    }

    private static Task buildTestResource() {
        return TaskResource.builder()
                           .id(RawCloudEntityTest.GUID_STRING)
                           .createdAt(RawCloudEntityTest.CREATED_AT_STRING)
                           .updatedAt(RawCloudEntityTest.UPDATED_AT_STRING)
                           .name(NAME)
                           .command(COMMAND)
                           .diskInMb(DISK_IN_MB)
                           .memoryInMb(MEMORY_IN_MB)
                           .state(STATE)
                           .result(buildTestResult())
                           .dropletId(DROPLET_ID)
                           .sequenceId(SEQUENCE_ID)
                           .build();
    }

    private static Result buildTestResult() {
        return Result.builder()
                     .failureReason(FAILURE_REASON)
                     .build();
    }

}
