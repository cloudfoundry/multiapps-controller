package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.cloudfoundry.client.v3.processes.Data;
import org.cloudfoundry.client.v3.processes.HealthCheck;
import org.cloudfoundry.client.v3.processes.HealthCheckType;
import org.cloudfoundry.client.v3.processes.Process;
import org.cloudfoundry.client.v3.processes.ReadinessHealthCheck;
import org.cloudfoundry.client.v3.processes.ReadinessHealthCheckType;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudProcess;
import org.junit.jupiter.api.Test;

class RawCloudProcessTest {

    @Test
    void testDeriveCarriesLivenessIntervalFromHealthCheckData() {
        Process process = buildProcess(buildHealthCheck(15), buildReadinessHealthCheck(null));

        CloudProcess derived = ImmutableRawCloudProcess.of(process)
                                                       .derive();

        assertEquals(15, derived.getHealthCheckInterval());
    }

    @Test
    void testDeriveLivenessIntervalIsNullWhenAbsent() {
        Process process = buildProcess(buildHealthCheck(null), buildReadinessHealthCheck(null));

        CloudProcess derived = ImmutableRawCloudProcess.of(process)
                                                       .derive();

        assertNull(derived.getHealthCheckInterval());
    }

    @Test
    void testDeriveLivenessIntervalIsNullWhenHealthCheckDataIsNull() {
        HealthCheck healthCheck = mock(HealthCheck.class);
        when(healthCheck.getData()).thenReturn(null);
        when(healthCheck.getType()).thenReturn(HealthCheckType.PORT);
        Process process = buildProcess(healthCheck, buildReadinessHealthCheck(null));

        CloudProcess derived = ImmutableRawCloudProcess.of(process)
                                                       .derive();

        assertNull(derived.getHealthCheckInterval());
    }

    private static Process buildProcess(HealthCheck healthCheck, ReadinessHealthCheck readinessHealthCheck) {
        Process process = mock(Process.class);
        when(process.getCommand()).thenReturn("test-command");
        when(process.getInstances()).thenReturn(1);
        when(process.getMemoryInMb()).thenReturn(256);
        when(process.getDiskInMb()).thenReturn(512);
        when(process.getHealthCheck()).thenReturn(healthCheck);
        when(process.getReadinessHealthCheck()).thenReturn(readinessHealthCheck);
        return process;
    }

    private static HealthCheck buildHealthCheck(Integer interval) {
        Data.Builder dataBuilder = Data.builder();
        if (interval != null) {
            dataBuilder.interval(interval);
        }
        return HealthCheck.builder()
                          .type(HealthCheckType.PORT)
                          .data(dataBuilder.build())
                          .build();
    }

    private static ReadinessHealthCheck buildReadinessHealthCheck(Integer interval) {
        Data.Builder dataBuilder = Data.builder();
        if (interval != null) {
            dataBuilder.interval(interval);
        }
        return ReadinessHealthCheck.builder()
                                   .type(ReadinessHealthCheckType.PORT)
                                   .data(dataBuilder.build())
                                   .build();
    }
}
