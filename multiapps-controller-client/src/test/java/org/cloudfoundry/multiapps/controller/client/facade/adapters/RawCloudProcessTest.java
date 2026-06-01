package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.client.v3.processes.Data;
import org.cloudfoundry.client.v3.processes.HealthCheck;
import org.cloudfoundry.client.v3.processes.HealthCheckType;
import org.cloudfoundry.client.v3.processes.ProcessRelationships;
import org.cloudfoundry.client.v3.processes.ProcessResource;
import org.cloudfoundry.client.v3.processes.ReadinessHealthCheck;
import org.cloudfoundry.client.v3.processes.ReadinessHealthCheckType;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudProcess;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudProcess;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RawCloudProcessTest {

    private static final String COMMAND = "bundle exec rackup";
    private static final Integer DISK_IN_MB = 1024;
    private static final Integer INSTANCES = 2;
    private static final Integer MEMORY_IN_MB = 512;
    private static final String HTTP_ENDPOINT = "/health";
    private static final Integer TIMEOUT = 30;
    private static final Integer INVOCATION_TIMEOUT = 5;
    private static final Integer INTERVAL = 10;
    private static final String READINESS_HTTP_ENDPOINT = "/ready";
    private static final Integer READINESS_INVOCATION_TIMEOUT = 4;
    private static final Integer READINESS_INTERVAL = 8;

    @Test
    void testDeriveWithFullHealthCheckDataIncludingInterval() {
        RawCloudProcess raw = ImmutableRawCloudProcess.of(buildProcessResource(HealthCheckType.HTTP, buildHealthCheckData(),
                                                                               ReadinessHealthCheckType.HTTP,
                                                                               buildReadinessHealthCheckData()));

        CloudProcess derived = raw.derive();

        assertEquals(buildExpected(org.cloudfoundry.multiapps.controller.client.facade.domain.HealthCheckType.HTTP, INTERVAL,
                                   ReadinessHealthCheckType.HTTP.getValue()),
                     derived);
    }

    @Test
    void testDeriveWithMissingHealthCheckDataLeavesIntervalNull() {
        // healthCheck.getData() returns null -> all four health-check Integer/String fields stay null
        RawCloudProcess raw = ImmutableRawCloudProcess.of(buildProcessResource(HealthCheckType.PORT, null,
                                                                               ReadinessHealthCheckType.PORT, null));

        CloudProcess derived = raw.derive();

        assertEquals(org.cloudfoundry.multiapps.controller.client.facade.domain.HealthCheckType.PORT, derived.getHealthCheckType());
        assertNull(derived.getHealthCheckTimeout());
        assertNull(derived.getHealthCheckInvocationTimeout());
        assertNull(derived.getHealthCheckHttpEndpoint());
        assertNull(derived.getHealthCheckInterval());
        assertNull(derived.getReadinessHealthCheckInvocationTimeout());
        assertNull(derived.getReadinessHealthCheckHttpEndpoint());
        assertNull(derived.getReadinessHealthCheckInterval());
    }

    @Test
    void testDeriveWithHealthCheckDataPresentButIntervalNull() {
        Data dataWithoutInterval = Data.builder()
                                       .endpoint(HTTP_ENDPOINT)
                                       .timeout(TIMEOUT)
                                       .invocationTimeout(INVOCATION_TIMEOUT)
                                       .build();
        RawCloudProcess raw = ImmutableRawCloudProcess.of(buildProcessResource(HealthCheckType.HTTP, dataWithoutInterval,
                                                                               ReadinessHealthCheckType.PORT, null));

        CloudProcess derived = raw.derive();

        assertEquals(TIMEOUT, derived.getHealthCheckTimeout());
        assertEquals(INVOCATION_TIMEOUT, derived.getHealthCheckInvocationTimeout());
        assertEquals(HTTP_ENDPOINT, derived.getHealthCheckHttpEndpoint());
        assertNull(derived.getHealthCheckInterval());
    }

    private static ProcessResource buildProcessResource(HealthCheckType healthCheckType, Data healthCheckData,
                                                        ReadinessHealthCheckType readinessHealthCheckType, Data readinessHealthCheckData) {
        HealthCheck.Builder healthCheckBuilder = HealthCheck.builder()
                                                            .type(healthCheckType);
        if (healthCheckData != null) {
            healthCheckBuilder.data(healthCheckData);
        }
        ReadinessHealthCheck.Builder readinessBuilder = ReadinessHealthCheck.builder()
                                                                            .type(readinessHealthCheckType);
        if (readinessHealthCheckData != null) {
            readinessBuilder.data(readinessHealthCheckData);
        }
        return ProcessResource.builder()
                              .id(RawCloudEntityTest.GUID_STRING)
                              .createdAt(RawCloudEntityTest.CREATED_AT_STRING)
                              .updatedAt(RawCloudEntityTest.UPDATED_AT_STRING)
                              .command(COMMAND)
                              .diskInMb(DISK_IN_MB)
                              .instances(INSTANCES)
                              .memoryInMb(MEMORY_IN_MB)
                              .type("web")
                              .metadata(Metadata.builder()
                                                .build())
                              .relationships(ProcessRelationships.builder()
                                                                 .build())
                              .healthCheck(healthCheckBuilder.build())
                              .readinessHealthCheck(readinessBuilder.build())
                              .build();
    }

    private static Data buildHealthCheckData() {
        return Data.builder()
                   .endpoint(HTTP_ENDPOINT)
                   .timeout(TIMEOUT)
                   .invocationTimeout(INVOCATION_TIMEOUT)
                   .interval(INTERVAL)
                   .build();
    }

    private static Data buildReadinessHealthCheckData() {
        return Data.builder()
                   .endpoint(READINESS_HTTP_ENDPOINT)
                   .invocationTimeout(READINESS_INVOCATION_TIMEOUT)
                   .interval(READINESS_INTERVAL)
                   .build();
    }

    private static CloudProcess buildExpected(org.cloudfoundry.multiapps.controller.client.facade.domain.HealthCheckType type,
                                              Integer interval, String readinessHealthCheckType) {
        return ImmutableCloudProcess.builder()
                                    .command(COMMAND)
                                    .instances(INSTANCES)
                                    .memoryInMb(MEMORY_IN_MB)
                                    .diskInMb(DISK_IN_MB)
                                    .healthCheckType(type)
                                    .healthCheckHttpEndpoint(HTTP_ENDPOINT)
                                    .healthCheckTimeout(TIMEOUT)
                                    .healthCheckInvocationTimeout(INVOCATION_TIMEOUT)
                                    .healthCheckInterval(interval)
                                    .readinessHealthCheckType(readinessHealthCheckType)
                                    .readinessHealthCheckHttpEndpoint(READINESS_HTTP_ENDPOINT)
                                    .readinessHealthCheckInvocationTimeout(READINESS_INVOCATION_TIMEOUT)
                                    .readinessHealthCheckInterval(READINESS_INTERVAL)
                                    .build();
    }
}
