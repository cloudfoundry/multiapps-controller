package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import java.util.List;

import org.cloudfoundry.client.v3.Error;
import org.cloudfoundry.client.v3.jobs.GetJobResponse;
import org.cloudfoundry.client.v3.jobs.Job;
import org.cloudfoundry.client.v3.jobs.JobState;
import org.cloudfoundry.client.v3.jobs.Warning;
import org.junit.jupiter.api.Test;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudAsyncJob;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudAsyncJob;

class RawCloudAsyncJobTest {

    private static final String OPERATION = "service_broker.delete";
    private static final String WARNING_1 = "warning1";
    private static final String WARNING_2 = "warning2";
    private static final String WARNINGS = "warning1,warning2";
    private static final int ERROR_CODE_1 = 10008;
    private static final int ERROR_CODE_2 = 1000;
    private static final String ERROR_TITLE_1 = "CF-UnprocessableEntity";
    private static final String ERROR_TITLE_2 = "CF-InvalidAuthToken";
    private static final String ERROR_DETAIL_1 = "something went wrong";
    private static final String ERROR_DETAIL_2 = "not valid token";
    private static final String ERRORS = "10008 CF-UnprocessableEntity something went wrong,1000 CF-InvalidAuthToken not valid token";

    @Test
    void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedTask(), buildRawTask());
    }

    private CloudAsyncJob buildExpectedTask() {
        return ImmutableCloudAsyncJob.builder()
                                     .metadata(RawCloudEntityTest.EXPECTED_METADATA_PARSED_FROM_V3_RESOURCE)
                                     .state(JobState.FAILED)
                                     .operation(OPERATION)
                                     .warnings(WARNINGS)
                                     .errors(ERRORS)
                                     .build();
    }

    private RawCloudAsyncJob buildRawTask() {
        return ImmutableRawCloudAsyncJob.of(buildTestResource());
    }

    private Job buildTestResource() {
        return GetJobResponse.builder()
                             .id(RawCloudEntityTest.GUID_STRING)
                             .createdAt(RawCloudEntityTest.CREATED_AT_STRING)
                             .updatedAt(RawCloudEntityTest.UPDATED_AT_STRING)
                             .state(JobState.FAILED)
                             .operation(OPERATION)
                             .warnings(buildWarnings())
                             .errors(buildErrors())
                             .build();
    }

    private List<Warning> buildWarnings() {
        return List.of(Warning.builder()
                              .detail(WARNING_1)
                              .build(),
                       Warning.builder()
                              .detail(WARNING_2)
                              .build());
    }

    private List<Error> buildErrors() {
        return List.of(Error.builder()
                            .code(ERROR_CODE_1)
                            .title(ERROR_TITLE_1)
                            .detail(ERROR_DETAIL_1)
                            .build(),
                       Error.builder()
                            .code(ERROR_CODE_2)
                            .title(ERROR_TITLE_2)
                            .detail(ERROR_DETAIL_2)
                            .build());
    }

}
