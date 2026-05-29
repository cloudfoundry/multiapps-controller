package org.cloudfoundry.multiapps.controller.client.lib.domain;

import org.cloudfoundry.multiapps.controller.client.facade.domain.HealthCheckType;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudProcess;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableStaging;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class HealthCheckInfoTest {

    @Test
    void testEqualHealthCheckInfoObjectsAreEqual() {
        ImmutableStaging staging = ImmutableStaging.builder()
                                                   .healthCheckType("http")
                                                   .healthCheckInterval(15)
                                                   .build();
        HealthCheckInfo first = HealthCheckInfo.fromStaging(staging);
        HealthCheckInfo second = HealthCheckInfo.fromStaging(staging);

        assertEquals(first, second);
    }

    @Test
    void testHealthCheckInfoWithDifferentIntervalsAreNotEqual() {
        ImmutableStaging stagingWithInterval15 = ImmutableStaging.builder()
                                                                 .healthCheckType("http")
                                                                 .healthCheckInterval(15)
                                                                 .build();
        ImmutableStaging stagingWithInterval30 = ImmutableStaging.builder()
                                                                 .healthCheckType("http")
                                                                 .healthCheckInterval(30)
                                                                 .build();

        HealthCheckInfo first = HealthCheckInfo.fromStaging(stagingWithInterval15);
        HealthCheckInfo second = HealthCheckInfo.fromStaging(stagingWithInterval30);

        assertNotEquals(first, second);
    }

    @Test
    void testHealthCheckInfoIntervalNullVsNonNullAreNotEqual() {
        ImmutableStaging stagingWithInterval = ImmutableStaging.builder()
                                                               .healthCheckType("http")
                                                               .healthCheckInterval(15)
                                                               .build();
        ImmutableStaging stagingWithoutInterval = ImmutableStaging.builder()
                                                                  .healthCheckType("http")
                                                                  .build();

        HealthCheckInfo withInterval = HealthCheckInfo.fromStaging(stagingWithInterval);
        HealthCheckInfo withoutInterval = HealthCheckInfo.fromStaging(stagingWithoutInterval);

        assertNotEquals(withInterval, withoutInterval);
    }

    @Test
    void testHealthCheckInfoFromProcessWithInterval() {
        ImmutableCloudProcess process = ImmutableCloudProcess.builder()
                                                             .command("start")
                                                             .diskInMb(256)
                                                             .instances(1)
                                                             .memoryInMb(512)
                                                             .healthCheckType(HealthCheckType.HTTP)
                                                             .healthCheckInterval(15)
                                                             .build();

        HealthCheckInfo fromProcess = HealthCheckInfo.fromProcess(process);

        ImmutableStaging staging = ImmutableStaging.builder()
                                                   .healthCheckType("http")
                                                   .healthCheckInterval(15)
                                                   .build();
        HealthCheckInfo fromStaging = HealthCheckInfo.fromStaging(staging);

        assertEquals(fromProcess, fromStaging);
    }
}
