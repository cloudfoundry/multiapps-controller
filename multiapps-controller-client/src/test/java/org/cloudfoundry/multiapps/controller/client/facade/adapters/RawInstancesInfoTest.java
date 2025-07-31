package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import org.cloudfoundry.client.v3.applications.GetApplicationProcessStatisticsResponse;
import org.cloudfoundry.client.v3.processes.ProcessState;
import org.cloudfoundry.client.v3.processes.ProcessStatisticsResource;
import org.junit.jupiter.api.Test;

import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableInstanceInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableInstancesInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.InstanceState;
import org.cloudfoundry.multiapps.controller.client.facade.domain.InstancesInfo;

class RawInstancesInfoTest {

    @Test
    void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedInstancesInfo(), buildActualInstancesInfo());
    }

    private InstancesInfo buildExpectedInstancesInfo() {
        return ImmutableInstancesInfo.builder()
                                     .addInstance(ImmutableInstanceInfo.builder()
                                                                       .index(0)
                                                                       .state(InstanceState.RUNNING)
                                                                       .build())
                                     .build();
    }

    private RawInstancesInfo buildActualInstancesInfo() {
        return ImmutableRawInstancesInfo.builder()
                                        .processStatisticsResponse(getApplicationProcessStatisticsResponse())
                                        .build();
    }

    private GetApplicationProcessStatisticsResponse getApplicationProcessStatisticsResponse() {
        return GetApplicationProcessStatisticsResponse.builder()
                                                      .resource(ProcessStatisticsResource.builder()
                                                                                         .index(0)
                                                                                         .details("Instance is in running state")
                                                                                         .diskQuota(1024L)
                                                                                         .state(ProcessState.RUNNING)
                                                                                         .memoryQuota(1024L)
                                                                                         .type("web")
                                                                                         .uptime(9042L)
                                                                                         .fileDescriptorQuota(1024L)
                                                                                         .host("10.244.16.10")
                                                                                         .build())
                                                      .build();
    }

}
