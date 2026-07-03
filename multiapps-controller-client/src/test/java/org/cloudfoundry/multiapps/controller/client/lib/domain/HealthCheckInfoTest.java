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

    private static final String HTTP_TYPE = "http";
    private static final Integer TIMEOUT = 30;
    private static final Integer INVOCATION_TIMEOUT = 5;
    private static final Integer INTERVAL = 10;
    private static final String HTTP_ENDPOINT = "/health";

    @Test
    void testFromStagingPropagatesAllFieldsIncludingInterval() {
        Staging staging = ImmutableStaging.builder()
                                          .healthCheckType(HTTP_TYPE)
                                          .healthCheckTimeout(TIMEOUT)
                                          .invocationTimeout(INVOCATION_TIMEOUT)
                                          .healthCheckInterval(INTERVAL)
                                          .healthCheckHttpEndpoint(HTTP_ENDPOINT)
                                          .build();

        HealthCheckInfo info = HealthCheckInfo.fromStaging(staging);

        assertEquals(HTTP_TYPE, info.getType());
        assertEquals(TIMEOUT, info.getTimeout());
        assertEquals(INVOCATION_TIMEOUT, info.getInvocationTimeout());
        assertEquals(INTERVAL, info.getInterval());
        assertEquals(HTTP_ENDPOINT, info.getHttpEndpoint());
    }

    @Test
    void testFromStagingDefaultsTypeToPortWhenNullAndCarriesNullInterval() {
        Staging staging = ImmutableStaging.builder()
                                          .build();

        HealthCheckInfo info = HealthCheckInfo.fromStaging(staging);

        assertEquals("port", info.getType());
        assertNull(info.getTimeout());
        assertNull(info.getInvocationTimeout());
        assertNull(info.getInterval());
        assertNull(info.getHttpEndpoint());
    }

    @Test
    void testFromProcessPropagatesAllFieldsIncludingInterval() {
        CloudProcess process = ImmutableCloudProcess.builder()
                                                    .command("cmd")
                                                    .diskInMb(512)
                                                    .instances(1)
                                                    .memoryInMb(1024)
                                                    .healthCheckType(HealthCheckType.HTTP)
                                                    .healthCheckHttpEndpoint(HTTP_ENDPOINT)
                                                    .healthCheckTimeout(TIMEOUT)
                                                    .healthCheckInvocationTimeout(INVOCATION_TIMEOUT)
                                                    .healthCheckInterval(INTERVAL)
                                                    .build();

        HealthCheckInfo info = HealthCheckInfo.fromProcess(process);

        assertEquals(HealthCheckType.HTTP.toString(), info.getType());
        assertEquals(TIMEOUT, info.getTimeout());
        assertEquals(INVOCATION_TIMEOUT, info.getInvocationTimeout());
        assertEquals(INTERVAL, info.getInterval());
        assertEquals(HTTP_ENDPOINT, info.getHttpEndpoint());
    }

    @Test
    void testFromProcessWithNullIntervalCarriesNull() {
        CloudProcess process = ImmutableCloudProcess.builder()
                                                    .command("cmd")
                                                    .diskInMb(512)
                                                    .instances(1)
                                                    .memoryInMb(1024)
                                                    .healthCheckType(HealthCheckType.PORT)
                                                    .build();

        HealthCheckInfo info = HealthCheckInfo.fromProcess(process);

        assertEquals(HealthCheckType.PORT.toString(), info.getType());
        assertNull(info.getInterval());
    }

    @Test
    void testEqualsReturnsTrueForIdenticalContent() {
        Staging staging = ImmutableStaging.builder()
                                          .healthCheckType(HTTP_TYPE)
                                          .healthCheckTimeout(TIMEOUT)
                                          .invocationTimeout(INVOCATION_TIMEOUT)
                                          .healthCheckInterval(INTERVAL)
                                          .healthCheckHttpEndpoint(HTTP_ENDPOINT)
                                          .build();

        HealthCheckInfo a = HealthCheckInfo.fromStaging(staging);
        HealthCheckInfo b = HealthCheckInfo.fromStaging(staging);

        assertEquals(a, b);
    }

    @Test
    void testEqualsReturnsFalseWhenIntervalDiffers() {
        Staging stagingWith10 = ImmutableStaging.builder()
                                                .healthCheckType(HTTP_TYPE)
                                                .healthCheckTimeout(TIMEOUT)
                                                .invocationTimeout(INVOCATION_TIMEOUT)
                                                .healthCheckInterval(10)
                                                .healthCheckHttpEndpoint(HTTP_ENDPOINT)
                                                .build();
        Staging stagingWith20 = ImmutableStaging.builder()
                                                .healthCheckType(HTTP_TYPE)
                                                .healthCheckTimeout(TIMEOUT)
                                                .invocationTimeout(INVOCATION_TIMEOUT)
                                                .healthCheckInterval(20)
                                                .healthCheckHttpEndpoint(HTTP_ENDPOINT)
                                                .build();

        HealthCheckInfo a = HealthCheckInfo.fromStaging(stagingWith10);
        HealthCheckInfo b = HealthCheckInfo.fromStaging(stagingWith20);

        assertNotEquals(a, b);
    }

    @Test
    void testEqualsReturnsFalseWhenOneIntervalIsNull() {
        Staging stagingWithInterval = ImmutableStaging.builder()
                                                      .healthCheckType(HTTP_TYPE)
                                                      .healthCheckInterval(INTERVAL)
                                                      .build();
        Staging stagingWithoutInterval = ImmutableStaging.builder()
                                                         .healthCheckType(HTTP_TYPE)
                                                         .build();

        HealthCheckInfo a = HealthCheckInfo.fromStaging(stagingWithInterval);
        HealthCheckInfo b = HealthCheckInfo.fromStaging(stagingWithoutInterval);

        assertNotEquals(a, b);
    }

    @Test
    void testEqualsReturnsFalseForNonHealthCheckInfo() {
        HealthCheckInfo info = HealthCheckInfo.fromStaging(ImmutableStaging.builder()
                                                                           .build());

        assertNotEquals(info, "not-a-health-check-info");
    }

    @Test
    void testEqualsReturnsTrueForSelf() {
        HealthCheckInfo info = HealthCheckInfo.fromStaging(ImmutableStaging.builder()
                                                                           .healthCheckInterval(INTERVAL)
                                                                           .build());

        assertEquals(info, info);
    }
}
