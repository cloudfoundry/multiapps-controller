package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import org.cloudfoundry.client.v3.processes.Data;
import org.cloudfoundry.client.v3.processes.HealthCheck;
import org.cloudfoundry.client.v3.processes.HealthCheckType;
import org.cloudfoundry.client.v3.processes.Process;
import org.cloudfoundry.client.v3.processes.ReadinessHealthCheck;
import org.cloudfoundry.client.v3.processes.ReadinessHealthCheckType;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudProcess;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RawCloudProcessTest {

    private static final Integer HEALTH_CHECK_TIMEOUT = 60;
    private static final Integer HEALTH_CHECK_INVOCATION_TIMEOUT = 5;
    private static final Integer HEALTH_CHECK_INTERVAL = 15;
    private static final String HEALTH_CHECK_HTTP_ENDPOINT = "/health";
    private static final Integer READINESS_HEALTH_CHECK_INVOCATION_TIMEOUT = 4;
    private static final Integer READINESS_HEALTH_CHECK_INTERVAL = 30;
    private static final String READINESS_HEALTH_CHECK_HTTP_ENDPOINT = "/ready";
    private static final Integer INSTANCES = 2;
    private static final Integer MEMORY_IN_MB = 256;
    private static final Integer DISK_IN_MB = 512;
    private static final String COMMAND = "start.sh";

    @Test
    void testDeriveRoundTripsHealthCheckInterval() {
        Process process = buildProcess(buildHealthCheckWithInterval(HEALTH_CHECK_INTERVAL),
                                       buildReadinessHealthCheckWithInterval(READINESS_HEALTH_CHECK_INTERVAL));

        CloudProcess cloudProcess = ImmutableRawCloudProcess.of(process)
                                                            .derive();

        assertEquals(HEALTH_CHECK_INTERVAL, cloudProcess.getHealthCheckInterval());
    }

    @Test
    void testDeriveHealthCheckIntervalIsNullWhenHealthCheckDataIsNull() {
        Process process = buildProcess(buildHealthCheckWithoutData(),
                                       buildReadinessHealthCheckWithInterval(READINESS_HEALTH_CHECK_INTERVAL));

        CloudProcess cloudProcess = ImmutableRawCloudProcess.of(process)
                                                            .derive();

        assertNull(cloudProcess.getHealthCheckInterval());
    }

    @Test
    void testDeriveHealthCheckIntervalIsNullWhenIntervalNotSetOnData() {
        Process process = buildProcess(buildHealthCheckWithInterval(null),
                                       buildReadinessHealthCheckWithInterval(READINESS_HEALTH_CHECK_INTERVAL));

        CloudProcess cloudProcess = ImmutableRawCloudProcess.of(process)
                                                            .derive();

        assertNull(cloudProcess.getHealthCheckInterval());
    }

    @Test
    void testDeriveHealthCheckIntervalDoesNotCollideWithReadinessInterval() {
        Process process = buildProcess(buildHealthCheckWithInterval(HEALTH_CHECK_INTERVAL),
                                       buildReadinessHealthCheckWithInterval(READINESS_HEALTH_CHECK_INTERVAL));

        CloudProcess cloudProcess = ImmutableRawCloudProcess.of(process)
                                                            .derive();

        assertEquals(HEALTH_CHECK_INTERVAL, cloudProcess.getHealthCheckInterval());
        assertEquals(READINESS_HEALTH_CHECK_INTERVAL, cloudProcess.getReadinessHealthCheckInterval());
    }

    private static Process buildProcess(HealthCheck healthCheck, ReadinessHealthCheck readinessHealthCheck) {
        Process process = Mockito.mock(Process.class);
        Mockito.when(process.getHealthCheck())
               .thenReturn(healthCheck);
        Mockito.when(process.getReadinessHealthCheck())
               .thenReturn(readinessHealthCheck);
        Mockito.when(process.getCommand())
               .thenReturn(COMMAND);
        Mockito.when(process.getInstances())
               .thenReturn(INSTANCES);
        Mockito.when(process.getMemoryInMb())
               .thenReturn(MEMORY_IN_MB);
        Mockito.when(process.getDiskInMb())
               .thenReturn(DISK_IN_MB);
        return process;
    }

    private static HealthCheck buildHealthCheckWithInterval(Integer interval) {
        return HealthCheck.builder()
                          .type(HealthCheckType.HTTP)
                          .data(Data.builder()
                                    .endpoint(HEALTH_CHECK_HTTP_ENDPOINT)
                                    .timeout(HEALTH_CHECK_TIMEOUT)
                                    .invocationTimeout(HEALTH_CHECK_INVOCATION_TIMEOUT)
                                    .interval(interval)
                                    .build())
                          .build();
    }

    private static HealthCheck buildHealthCheckWithoutData() {
        return HealthCheck.builder()
                          .type(HealthCheckType.PROCESS)
                          .build();
    }

    private static ReadinessHealthCheck buildReadinessHealthCheckWithInterval(Integer interval) {
        return ReadinessHealthCheck.builder()
                                   .type(ReadinessHealthCheckType.HTTP)
                                   .data(Data.builder()
                                             .endpoint(READINESS_HEALTH_CHECK_HTTP_ENDPOINT)
                                             .invocationTimeout(READINESS_HEALTH_CHECK_INVOCATION_TIMEOUT)
                                             .interval(interval)
                                             .build())
                                   .build();
    }

}
