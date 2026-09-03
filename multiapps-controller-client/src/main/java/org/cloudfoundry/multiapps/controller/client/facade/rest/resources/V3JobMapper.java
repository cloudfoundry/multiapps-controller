package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.List;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudAsyncJob;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudAsyncJob;
import org.cloudfoundry.multiapps.controller.client.facade.domain.JobState;

public final class V3JobMapper {

    private V3JobMapper() {
    }

    public static CloudAsyncJob toCloudAsyncJob(V3Job job) {
        return ImmutableCloudAsyncJob.builder()
                                     .metadata(V3ResourceMappers.parseMetadata(job.guid(), job.createdAt(), job.updatedAt()))
                                     .state(parseState(job.state()))
                                     .operation(job.operation())
                                     .warnings(getWarnings(job))
                                     .errors(getErrors(job))
                                     .build();
    }

    private static JobState parseState(String state) {
        return state == null ? null : JobState.valueOf(state.toUpperCase());
    }

    private static String getWarnings(V3Job job) {
        List<V3Job.V3Warning> warnings = job.warnings();
        if (warnings == null) {
            return "";
        }

        return warnings.stream()
                       .map(V3Job.V3Warning::detail)
                       .collect(Collectors.joining(","));
    }

    private static String getErrors(V3Job job) {
        List<V3Job.V3Error> errors = job.errors();
        if (errors == null) {
            return "";
        }

        return errors.stream()
                     .map(V3JobMapper::joinErrorDetails)
                     .collect(Collectors.joining(","));
    }

    private static String joinErrorDetails(V3Job.V3Error error) {
        return String.join(" ", String.valueOf(error.code()), error.title(), error.detail());
    }

}
