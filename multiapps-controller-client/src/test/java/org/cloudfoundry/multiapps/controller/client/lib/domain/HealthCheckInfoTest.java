package org.cloudfoundry.multiapps.controller.client.lib.domain;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudProcess;
import org.cloudfoundry.multiapps.controller.client.facade.domain.HealthCheckType;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudProcess;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableStaging;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Staging;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HealthCheckInfoTest {

    @Test
    void testFromStagingCarriesInterval() {
        Staging staging = ImmutableStaging.builder()
                                          .healthCheckType("http")
                                          .healthCheckTimeout(30)
                                          .invocationTimeout(5)
                                          .healthCheckInterval(15)
                                          .healthCheckHttpEndpoint("/health")
                                          .build();

        HealthCheckInfo info = HealthCheckInfo.fromStaging(staging);

        assertEquals("http", info.getType());
        assertEquals(30, info.getTimeout());
        assertEquals(5, info.getInvocationTimeout());
        assertEquals(15, info.getInterval());
        assertEquals("/health", info.getHttpEndpoint());
    }

    @Test
    void testFromStagingIntervalIsNullWhenAbsent() {
        Staging staging = ImmutableStaging.builder()
                                          .healthCheckType("port")
                                          .build();

        HealthCheckInfo info = HealthCheckInfo.fromStaging(staging);

        assertNull(info.getInterval());
    }

    @Test
    void testFromStagingDefaultsTypeToPortWhenNull() {
        Staging staging = ImmutableStaging.builder()
                                          .healthCheckInterval(10)
                                          .build();

        HealthCheckInfo info = HealthCheckInfo.fromStaging(staging);

        assertEquals("port", info.getType());
        assertEquals(10, info.getInterval());
    }

    @Test
    void testFromProcessCarriesInterval() {
        CloudProcess process = ImmutableCloudProcess.builder()
                                                    .command("test-cmd")
                                                    .diskInMb(512)
                                                    .instances(1)
                                                    .memoryInMb(256)
                                                    .healthCheckType(HealthCheckType.HTTP)
                                                    .healthCheckTimeout(45)
                                                    .healthCheckInvocationTimeout(7)
                                                    .healthCheckInterval(20)
                                                    .healthCheckHttpEndpoint("/live")
                                                    .build();

        HealthCheckInfo info = HealthCheckInfo.fromProcess(process);

        assertEquals("http", info.getType());
        assertEquals(45, info.getTimeout());
        assertEquals(7, info.getInvocationTimeout());
        assertEquals(20, info.getInterval());
        assertEquals("/live", info.getHttpEndpoint());
    }

    @Test
    void testFromProcessIntervalIsNullWhenAbsent() {
        CloudProcess process = ImmutableCloudProcess.builder()
                                                    .command("test-cmd")
                                                    .diskInMb(512)
                                                    .instances(1)
                                                    .memoryInMb(256)
                                                    .healthCheckType(HealthCheckType.PORT)
                                                    .build();

        HealthCheckInfo info = HealthCheckInfo.fromProcess(process);

        assertNull(info.getInterval());
    }

    @Test
    void testEqualsReturnsFalseWhenIntervalDiffers() {
        Staging stagingA = ImmutableStaging.builder()
                                           .healthCheckType("port")
                                           .healthCheckInterval(10)
                                           .build();
        Staging stagingB = ImmutableStaging.builder()
                                           .healthCheckType("port")
                                           .healthCheckInterval(20)
                                           .build();

        assertNotEquals(HealthCheckInfo.fromStaging(stagingA), HealthCheckInfo.fromStaging(stagingB));
    }

    @Test
    void testEqualsReturnsTrueWhenAllFieldsMatchIncludingInterval() {
        Staging stagingA = ImmutableStaging.builder()
                                           .healthCheckType("port")
                                           .healthCheckTimeout(30)
                                           .invocationTimeout(5)
                                           .healthCheckInterval(10)
                                           .healthCheckHttpEndpoint("/health")
                                           .build();
        Staging stagingB = ImmutableStaging.builder()
                                           .healthCheckType("port")
                                           .healthCheckTimeout(30)
                                           .invocationTimeout(5)
                                           .healthCheckInterval(10)
                                           .healthCheckHttpEndpoint("/health")
                                           .build();

        assertEquals(HealthCheckInfo.fromStaging(stagingA), HealthCheckInfo.fromStaging(stagingB));
    }

    @Test
    void testEqualsReturnsFalseWhenOneIntervalIsNull() {
        Staging stagingWithInterval = ImmutableStaging.builder()
                                                      .healthCheckType("port")
                                                      .healthCheckInterval(10)
                                                      .build();
        Staging stagingWithoutInterval = ImmutableStaging.builder()
                                                         .healthCheckType("port")
                                                         .build();

        assertNotEquals(HealthCheckInfo.fromStaging(stagingWithInterval), HealthCheckInfo.fromStaging(stagingWithoutInterval));
    }
}
