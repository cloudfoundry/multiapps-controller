package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import java.util.Optional;

import org.cloudfoundry.client.v3.tasks.Result;
import org.cloudfoundry.client.v3.tasks.Task;
import org.cloudfoundry.client.v3.tasks.TaskState;
import org.immutables.value.Value;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudTask;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudTask;

@Value.Immutable
public abstract class RawCloudTask extends RawCloudEntity<CloudTask> {

    @Value.Parameter
    public abstract Task getResource();

    @Override
    public CloudTask derive() {
        Task resource = getResource();
        return ImmutableCloudTask.builder()
                                 .metadata(parseResourceMetadata(resource))
                                 .name(resource.getName())
                                 .command(resource.getCommand())
                                 .limits(parseLimits(resource))
                                 .result(parseResult(resource))
                                 .state(parseState(resource.getState()))
                                 .build();
    }

    private static CloudTask.Result parseResult(Task resource) {
        return Optional.ofNullable(resource.getResult())
                       .map(Result::getFailureReason)
                       .map(ImmutableCloudTask.ImmutableResult::of)
                       .orElse(null);
    }

    private static CloudTask.Limits parseLimits(Task resource) {
        return ImmutableCloudTask.ImmutableLimits.builder()
                                                 .disk(resource.getDiskInMb())
                                                 .memory(resource.getMemoryInMb())
                                                 .build();
    }

    private static CloudTask.State parseState(TaskState state) {
        return parseEnum(state, CloudTask.State.class);
    }

}
