package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import org.cloudfoundry.client.v3.applications.GetApplicationProcessStatisticsResponse;
import org.cloudfoundry.client.v3.processes.ProcessState;
import org.cloudfoundry.client.v3.processes.ProcessStatisticsResource;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableInstanceInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableInstancesInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.InstanceState;
import org.cloudfoundry.multiapps.controller.client.facade.domain.InstancesInfo;
import org.junit.jupiter.api.Test;

class RawInstancesInfoTest {

    @Test
    void testDeriveWithTrueRoutable() {
        RawCloudEntityTest.testDerive(buildExpectedInstancesInfo(true), buildActualInstancesInfo("true"));
    }

    @Test
    void testDeriveWithFalseRoutable() {
        RawCloudEntityTest.testDerive(buildExpectedInstancesInfo(false), buildActualInstancesInfo("false"));
    }

    private InstancesInfo buildExpectedInstancesInfo(boolean expectedRoutable) {
        return ImmutableInstancesInfo.builder()
                                     .addInstance(ImmutableInstanceInfo.builder()
                                                                       .index(0)
                                                                       .state(InstanceState.RUNNING)
                                                                       .isRoutable(expectedRoutable)
                                                                       .build())
                                     .build();
    }

    private RawInstancesInfo buildActualInstancesInfo(String routable) {
        return ImmutableRawInstancesInfo.builder()
                                        .processStatisticsResponse(getApplicationProcessStatisticsResponse(routable))
                                        .build();
    }

    private GetApplicationProcessStatisticsResponse getApplicationProcessStatisticsResponse(String routable) {
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
                                                                                         .routable(routable)
                                                                                         .host("10.244.16.10")
                                                                                         .build())
                                                      .build();
    }

}
