package org.cloudfoundry.multiapps.controller.client.lib.domain;

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
    private static final String PORT_TYPE = "port";
    private static final String ENDPOINT = "/health";
    private static final Integer TIMEOUT = 10;
    private static final Integer INVOCATION_TIMEOUT = 5;
    private static final Integer INTERVAL = 30;

    @Test
    void testFromStagingExposesAllFieldsIncludingInterval() {
        Staging staging = ImmutableStaging.builder()
                                          .healthCheckType(HTTP_TYPE)
                                          .healthCheckTimeout(TIMEOUT)
                                          .invocationTimeout(INVOCATION_TIMEOUT)
                                          .healthCheckHttpEndpoint(ENDPOINT)
                                          .healthCheckInterval(INTERVAL)
                                          .build();

        HealthCheckInfo info = HealthCheckInfo.fromStaging(staging);

        assertEquals(HTTP_TYPE, info.getType());
        assertEquals(TIMEOUT, info.getTimeout());
        assertEquals(INVOCATION_TIMEOUT, info.getInvocationTimeout());
        assertEquals(ENDPOINT, info.getHttpEndpoint());
        assertEquals(INTERVAL, info.getInterval());
    }

    @Test
    void testFromStagingDefaultsTypeToPortWhenAbsent() {
        Staging staging = ImmutableStaging.builder()
                                          .build();

        HealthCheckInfo info = HealthCheckInfo.fromStaging(staging);

        assertEquals(PORT_TYPE, info.getType());
        assertNull(info.getTimeout());
        assertNull(info.getInvocationTimeout());
        assertNull(info.getHttpEndpoint());
        assertNull(info.getInterval());
    }

    @Test
    void testFromStagingPropagatesNullInterval() {
        Staging staging = ImmutableStaging.builder()
                                          .healthCheckType(HTTP_TYPE)
                                          .build();

        HealthCheckInfo info = HealthCheckInfo.fromStaging(staging);

        assertEquals(HTTP_TYPE, info.getType());
        assertNull(info.getInterval());
    }

    @Test
    void testFromProcessExposesAllFieldsIncludingInterval() {
        ImmutableCloudProcess process = ImmutableCloudProcess.builder()
                                                             .command("./run.sh")
                                                             .diskInMb(1024)
                                                             .instances(1)
                                                             .memoryInMb(512)
                                                             .healthCheckType(HealthCheckType.HTTP)
                                                             .healthCheckHttpEndpoint(ENDPOINT)
                                                             .healthCheckTimeout(TIMEOUT)
                                                             .healthCheckInvocationTimeout(INVOCATION_TIMEOUT)
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
        ImmutableCloudProcess process = ImmutableCloudProcess.builder()
                                                             .command("./run.sh")
                                                             .diskInMb(1024)
                                                             .instances(1)
                                                             .memoryInMb(512)
                                                             .healthCheckType(HealthCheckType.PORT)
                                                             .build();

        HealthCheckInfo info = HealthCheckInfo.fromProcess(process);

        assertEquals(HealthCheckType.PORT.toString(), info.getType());
        assertNull(info.getInterval());
    }

    @Test
    void testEqualsReturnsTrueForSameValues() {
        HealthCheckInfo first = HealthCheckInfo.fromStaging(buildStagingWithInterval(INTERVAL));
        HealthCheckInfo second = HealthCheckInfo.fromStaging(buildStagingWithInterval(INTERVAL));

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    private static Staging buildStagingWithInterval(Integer interval) {
        return ImmutableStaging.builder()
                               .healthCheckType(HTTP_TYPE)
                               .healthCheckTimeout(TIMEOUT)
                               .invocationTimeout(INVOCATION_TIMEOUT)
                               .healthCheckHttpEndpoint(ENDPOINT)
                               .healthCheckInterval(interval)
                               .build();
    }

    @Test
    void testEqualsReturnsFalseWhenIntervalDiffers() {
        HealthCheckInfo first = HealthCheckInfo.fromStaging(buildStagingWithInterval(30));
        HealthCheckInfo second = HealthCheckInfo.fromStaging(buildStagingWithInterval(60));

        assertNotEquals(first, second);
    }

    @Test
    void testEqualsReturnsFalseWhenIntervalIsNullOnOneSide() {
        HealthCheckInfo first = HealthCheckInfo.fromStaging(buildStagingWithInterval(INTERVAL));
        HealthCheckInfo second = HealthCheckInfo.fromStaging(buildStagingWithInterval(null));

        assertNotEquals(first, second);
    }

    @Test
    void testEqualsReturnsTrueForReferenceIdentity() {
        HealthCheckInfo info = HealthCheckInfo.fromStaging(buildStagingWithInterval(INTERVAL));

        assertEquals(info, info);
    }

    @Test
    void testEqualsReturnsFalseForUnrelatedType() {
        HealthCheckInfo info = HealthCheckInfo.fromStaging(buildStagingWithInterval(INTERVAL));

        assertNotEquals(info, "not-a-health-check-info");
    }

    @Test
    void testHashCodeRemainsStableAcrossInvocations() {
        HealthCheckInfo info = HealthCheckInfo.fromStaging(buildStagingWithInterval(INTERVAL));

        int firstHash = info.hashCode();
        int secondHash = info.hashCode();

        assertEquals(firstHash, secondHash);
    }
}
