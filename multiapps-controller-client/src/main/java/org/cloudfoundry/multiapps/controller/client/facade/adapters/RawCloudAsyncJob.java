package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import java.util.stream.Collectors;

import org.cloudfoundry.client.v3.jobs.Job;
import org.cloudfoundry.client.v3.jobs.Warning;
import org.immutables.value.Value;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudAsyncJob;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudAsyncJob;

@Value.Immutable
public abstract class RawCloudAsyncJob extends RawCloudEntity<CloudAsyncJob> {

    @Value.Parameter
    public abstract Job getJob();

    @Override
    public CloudAsyncJob derive() {
        Job job = getJob();
        return ImmutableCloudAsyncJob.builder()
                                     .metadata(parseResourceMetadata(job))
                                     .state(job.getState())
                                     .operation(job.getOperation())
                                     .warnings(getWarnings(job))
                                     .errors(getErrors(job))
                                     .build();
    }

    private String getWarnings(Job job) {
        return job.getWarnings()
                  .stream()
                  .map(Warning::getDetail)
                  .collect(Collectors.joining(","));
    }

    private String getErrors(Job job) {
        return job.getErrors()
                  .stream()
                  .map(this::joinErrorDetails)
                  .collect(Collectors.joining(","));
    }

    private String joinErrorDetails(org.cloudfoundry.client.v3.Error error) {
        return String.join(" ", error.getCode()
                                     .toString(),
                           error.getTitle(), error.getDetail());
    }

}
