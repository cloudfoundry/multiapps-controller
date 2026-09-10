package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.List;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudAsyncJob;
import org.cloudfoundry.multiapps.controller.client.facade.domain.JobState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class V3JobMapperTest {

    private static final String GUID_STRING = "3725a721-6e3f-4b6e-8f2b-1c2d3e4f5a6b";

    @Test
    void testToCloudAsyncJobMapsAllFields() {
        V3Job job = new V3Job(GUID_STRING, "2026-08-04T10:15:30Z", "2026-08-05T11:16:31Z", "PROCESSING", "app.apply_manifest",
                              List.of(new V3Job.V3Error("Something failed", "CF-Error", 10001)),
                              List.of(new V3Job.V3Warning("A warning")));

        CloudAsyncJob result = V3JobMapper.toCloudAsyncJob(job);

        Assertions.assertEquals(JobState.PROCESSING, result.getState());
        Assertions.assertEquals("app.apply_manifest", result.getOperation());
        Assertions.assertEquals("A warning", result.getWarnings());
        Assertions.assertEquals("10001 CF-Error Something failed", result.getErrors());
        Assertions.assertEquals(GUID_STRING, result.getGuid()
                                                   .toString());
    }

    @Test
    void testToCloudAsyncJobParseStateIsCaseInsensitive() {
        V3Job job = buildJobWithState("complete");

        Assertions.assertEquals(JobState.COMPLETE, V3JobMapper.toCloudAsyncJob(job)
                                                              .getState());
    }

    @Test
    void testToCloudAsyncJobParseStateWithNullReturnsNullState() {
        V3Job job = buildJobWithState(null);

        Assertions.assertNull(V3JobMapper.toCloudAsyncJob(job)
                                         .getState());
    }

    @Test
    void testToCloudAsyncJobParseStateWithUnknownValueThrows() {
        V3Job job = buildJobWithState("bogus");

        Assertions.assertThrows(IllegalArgumentException.class, () -> V3JobMapper.toCloudAsyncJob(job));
    }

    @Test
    void testToCloudAsyncJobNullWarningsReturnEmptyString() {
        V3Job job = new V3Job(GUID_STRING, null, null, "FAILED", null, List.of(), null);

        Assertions.assertEquals("", V3JobMapper.toCloudAsyncJob(job)
                                               .getWarnings());
    }

    @Test
    void testToCloudAsyncJobNullErrorsReturnEmptyString() {
        V3Job job = new V3Job(GUID_STRING, null, null, "FAILED", null, null, List.of());

        Assertions.assertEquals("", V3JobMapper.toCloudAsyncJob(job)
                                               .getErrors());
    }

    @Test
    void testToCloudAsyncJobMultipleWarningsAreJoinedWithComma() {
        V3Job job = new V3Job(GUID_STRING, null, null, "POLLING", null, null,
                              List.of(new V3Job.V3Warning("first warning"), new V3Job.V3Warning("second warning")));

        Assertions.assertEquals("first warning,second warning", V3JobMapper.toCloudAsyncJob(job)
                                                                           .getWarnings());
    }

    @Test
    void testToCloudAsyncJobMultipleErrorsAreJoinedWithComma() {
        V3Job job = new V3Job(GUID_STRING, null, null, "FAILED", null,
                              List.of(new V3Job.V3Error("detail error one", "Title1", 1),
                                      new V3Job.V3Error("detail error two", "Title2", 2)), null);

        Assertions.assertEquals("1 Title1 detail error one,2 Title2 detail error two", V3JobMapper.toCloudAsyncJob(job)
                                                                                                  .getErrors());
    }

    private static V3Job buildJobWithState(String state) {
        return new V3Job(GUID_STRING, null, null, state, null, null, null);
    }

}
