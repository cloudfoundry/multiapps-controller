package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudTask;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudTask;

/**
 * Maps the {@link V3Task} wire model to the project's {@link CloudTask} domain object. Mirrors the OSS {@code RawCloudTask} adapter
 * field-for-field, so both client implementations yield identical domain objects.
 */
public final class V3TaskMapper {

    private V3TaskMapper() {
    }

    public static CloudTask toCloudTask(V3Task task) {
        return ImmutableCloudTask.builder()
                                 .metadata(V3ResourceMappers.parseMetadata(task.guid(), task.createdAt(), task.updatedAt()))
                                 .name(task.name())
                                 .command(task.command())
                                 .limits(parseLimits(task))
                                 .result(parseResult(task))
                                 .state(parseState(task.state()))
                                 .build();
    }

    private static CloudTask.Result parseResult(V3Task task) {
        if (task.result() == null) {
            return null;
        }
        return ImmutableCloudTask.ImmutableResult.of(task.result()
                                                         .failureReason());
    }

    private static CloudTask.Limits parseLimits(V3Task task) {
        return ImmutableCloudTask.ImmutableLimits.builder()
                                                 .disk(task.diskInMb())
                                                 .memory(task.memoryInMb())
                                                 .build();
    }

    private static CloudTask.State parseState(String state) {
        return state == null ? null : CloudTask.State.valueOf(state);
    }

}
