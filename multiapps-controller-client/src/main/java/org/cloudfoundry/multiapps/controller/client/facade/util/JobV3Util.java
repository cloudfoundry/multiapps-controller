package org.cloudfoundry.multiapps.controller.client.facade.util;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v3.ClientV3Exception;
import org.cloudfoundry.client.v3.Error;
import org.cloudfoundry.client.v3.jobs.GetJobRequest;
import org.cloudfoundry.client.v3.jobs.GetJobResponse;
import org.cloudfoundry.client.v3.jobs.Job;
import org.cloudfoundry.client.v3.jobs.JobState;
import org.cloudfoundry.util.DelayUtils;

import reactor.core.publisher.Mono;

public class JobV3Util {

    private JobV3Util() {
    }

    private static final Set<JobState> FINAL_STATES = EnumSet.of(JobState.COMPLETE, JobState.FAILED);

    public static Mono<Void> waitForCompletion(CloudFoundryClient cloudFoundryClient, Duration completionTimeout, String jobId) {
        return requestJobV3(cloudFoundryClient, jobId).filter(job -> FINAL_STATES.contains(job.getState()))
                                                      .repeatWhenEmpty(DelayUtils.exponentialBackOff(Duration.ofSeconds(1),
                                                                                                     Duration.ofSeconds(15),
                                                                                                     completionTimeout))
                                                      .filter(job -> JobState.FAILED == job.getState())
                                                      .flatMap(JobV3Util::getError);
    }

    private static Mono<GetJobResponse> requestJobV3(CloudFoundryClient cloudFoundryClient, String jobId) {
        return cloudFoundryClient.jobsV3()
                                 .get(GetJobRequest.builder()
                                                   .jobId(jobId)
                                                   .build());
    }

    private static Mono<Void> getError(Job job) {
        List<Error> errors = job.getErrors();
        // Status code must be set, otherwise it will throw NPE during getStatusCode() invocation
        return Mono.error(new ClientV3Exception(200, errors));
    }

}
