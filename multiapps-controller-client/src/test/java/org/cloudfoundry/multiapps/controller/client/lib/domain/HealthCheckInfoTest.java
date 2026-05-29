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

    private static final String HTTP = "http";
    private static final String PORT = "port";
    private static final String ENDPOINT = "/health";
    private static final Integer TIMEOUT = 60;
    private static final Integer INVOCATION_TIMEOUT = 5;
    private static final Integer INTERVAL = 30;

    @Test
    void testFromStagingPopulatesInterval() {
        Staging staging = ImmutableStaging.builder()
                                          .healthCheckType(HTTP)
                                          .healthCheckTimeout(TIMEOUT)
                                          .invocationTimeout(INVOCATION_TIMEOUT)
                                          .healthCheckHttpEndpoint(ENDPOINT)
                                          .healthCheckInterval(INTERVAL)
                                          .build();

        HealthCheckInfo info = HealthCheckInfo.fromStaging(staging);

        assertEquals(HTTP, info.getType());
        assertEquals(TIMEOUT, info.getTimeout());
        assertEquals(INVOCATION_TIMEOUT, info.getInvocationTimeout());
        assertEquals(ENDPOINT, info.getHttpEndpoint());
        assertEquals(INTERVAL, info.getInterval());
    }

    @Test
    void testFromStagingDefaultsTypeToPortWhenMissing() {
        Staging staging = ImmutableStaging.builder()
                                          .build();

        HealthCheckInfo info = HealthCheckInfo.fromStaging(staging);

        assertEquals(PORT, info.getType());
        assertNull(info.getTimeout());
        assertNull(info.getInvocationTimeout());
        assertNull(info.getHttpEndpoint());
        assertNull(info.getInterval());
    }

    @Test
    void testFromStagingPropagatesNullInterval() {
        Staging staging = ImmutableStaging.builder()
                                          .healthCheckType(HTTP)
                                          .build();

        HealthCheckInfo info = HealthCheckInfo.fromStaging(staging);

        assertNull(info.getInterval());
    }

    @Test
    void testFromProcessPopulatesInterval() {
        CloudProcess process = ImmutableCloudProcess.builder()
                                                    .command("")
                                                    .diskInMb(0)
                                                    .instances(1)
                                                    .memoryInMb(256)
                                                    .healthCheckType(HealthCheckType.HTTP)
                                                    .healthCheckTimeout(TIMEOUT)
                                                    .healthCheckInvocationTimeout(INVOCATION_TIMEOUT)
                                                    .healthCheckHttpEndpoint(ENDPOINT)
                                                    .healthCheckInterval(INTERVAL)
                                                    .build();

        HealthCheckInfo info = HealthCheckInfo.fromProcess(process);

        assertEquals(HealthCheckType.HTTP.toString(), info.getType());
        assertEquals(TIMEOUT, info.getTimeout());
        assertEquals(INVOCATION_TIMEOUT, info.getInvocationTimeout());
        assertEquals(ENDPOINT, info.getHttpEndpoint());
        assertEquals(INTERVAL, info.getInterval());
    }

    @Test
    void testFromProcessPropagatesNullInterval() {
        CloudProcess process = ImmutableCloudProcess.builder()
                                                    .command("")
                                                    .diskInMb(0)
                                                    .instances(1)
                                                    .memoryInMb(256)
                                                    .healthCheckType(HealthCheckType.PORT)
                                                    .build();

        HealthCheckInfo info = HealthCheckInfo.fromProcess(process);

        assertNull(info.getInterval());
    }

    @Test
    void testEqualsWhenAllFieldsMatchIncludingInterval() {
        HealthCheckInfo a = HealthCheckInfo.fromStaging(buildStagingWithInterval(INTERVAL));
        HealthCheckInfo b = HealthCheckInfo.fromStaging(buildStagingWithInterval(INTERVAL));

        assertEquals(a, b);
    }

    private static Staging buildStagingWithInterval(Integer interval) {
        return ImmutableStaging.builder()
                               .healthCheckType(HTTP)
                               .healthCheckTimeout(TIMEOUT)
                               .invocationTimeout(INVOCATION_TIMEOUT)
                               .healthCheckHttpEndpoint(ENDPOINT)
                               .healthCheckInterval(interval)
                               .build();
    }

    @Test
    void testEqualsWhenIntervalDiffers() {
        HealthCheckInfo a = HealthCheckInfo.fromStaging(buildStagingWithInterval(30));
        HealthCheckInfo b = HealthCheckInfo.fromStaging(buildStagingWithInterval(60));

        assertNotEquals(a, b);
    }

    @Test
    void testEqualsWhenOneIntervalIsNull() {
        HealthCheckInfo a = HealthCheckInfo.fromStaging(buildStagingWithInterval(INTERVAL));
        HealthCheckInfo b = HealthCheckInfo.fromStaging(buildStagingWithInterval(null));

        assertNotEquals(a, b);
    }

    @Test
    void testEqualsIsReflexive() {
        HealthCheckInfo info = HealthCheckInfo.fromStaging(buildStagingWithInterval(INTERVAL));

        assertEquals(info, info);
    }

    @Test
    void testEqualsWithDifferentClass() {
        HealthCheckInfo info = HealthCheckInfo.fromStaging(buildStagingWithInterval(INTERVAL));

        assertNotEquals(info, "not a HealthCheckInfo");
    }

}
