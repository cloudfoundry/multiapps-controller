package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.client.v3.Relationship;
import org.cloudfoundry.client.v3.ToOneRelationship;
import org.cloudfoundry.client.v3.processes.Data;
import org.cloudfoundry.client.v3.processes.HealthCheck;
import org.cloudfoundry.client.v3.processes.HealthCheckType;
import org.cloudfoundry.client.v3.processes.ProcessRelationships;
import org.cloudfoundry.client.v3.processes.ProcessResource;
import org.cloudfoundry.client.v3.processes.ReadinessHealthCheck;
import org.cloudfoundry.client.v3.processes.ReadinessHealthCheckType;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudProcess;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RawCloudProcessTest {

    private static final String PROCESS_ID = "0d6b6f94-2e9b-4f62-9c3a-bdf9a7e4d1d0";
    private static final String CREATED_AT = "2024-01-26T10:00:00Z";
    private static final String COMMAND = "start";
    private static final Integer INSTANCES = 2;
    private static final Integer MEMORY_IN_MB = 256;
    private static final Integer DISK_IN_MB = 1024;
    private static final Integer HEALTH_CHECK_TIMEOUT = 60;
    private static final Integer HEALTH_CHECK_INVOCATION_TIMEOUT = 5;
    private static final String HEALTH_CHECK_HTTP_ENDPOINT = "/health";
    private static final Integer HEALTH_CHECK_INTERVAL = 15;

    @Test
    void testDeriveMapsHealthCheckIntervalFromData() {
        RawCloudProcess raw = buildRawProcess(buildHealthCheck(HEALTH_CHECK_INTERVAL), buildReadinessHealthCheck());

        CloudProcess derived = raw.derive();

        assertEquals(HEALTH_CHECK_INTERVAL, derived.getHealthCheckInterval());
        assertEquals(HEALTH_CHECK_TIMEOUT, derived.getHealthCheckTimeout());
        assertEquals(HEALTH_CHECK_INVOCATION_TIMEOUT, derived.getHealthCheckInvocationTimeout());
        assertEquals(HEALTH_CHECK_HTTP_ENDPOINT, derived.getHealthCheckHttpEndpoint());
    }

    @Test
    void testDeriveLeavesHealthCheckIntervalNullWhenAbsentFromData() {
        HealthCheck healthCheck = HealthCheck.builder()
                                             .type(HealthCheckType.HTTP)
                                             .data(Data.builder()
                                                       .timeout(HEALTH_CHECK_TIMEOUT)
                                                       .invocationTimeout(HEALTH_CHECK_INVOCATION_TIMEOUT)
                                                       .endpoint(HEALTH_CHECK_HTTP_ENDPOINT)
                                                       .build())
                                             .build();
        RawCloudProcess raw = buildRawProcess(healthCheck, buildReadinessHealthCheck());

        CloudProcess derived = raw.derive();

        assertNull(derived.getHealthCheckInterval());
        assertEquals(HEALTH_CHECK_TIMEOUT, derived.getHealthCheckTimeout());
    }

    @Test
    void testDeriveLeavesHealthCheckIntervalNullWhenDataIsNull() {
        HealthCheck healthCheck = HealthCheck.builder()
                                             .type(HealthCheckType.PORT)
                                             .build();
        RawCloudProcess raw = buildRawProcess(healthCheck, buildReadinessHealthCheck());

        CloudProcess derived = raw.derive();

        assertNull(derived.getHealthCheckInterval());
        assertNull(derived.getHealthCheckTimeout());
        assertNull(derived.getHealthCheckInvocationTimeout());
        assertNull(derived.getHealthCheckHttpEndpoint());
    }

    private static HealthCheck buildHealthCheck(Integer interval) {
        return HealthCheck.builder()
                          .type(HealthCheckType.HTTP)
                          .data(Data.builder()
                                    .timeout(HEALTH_CHECK_TIMEOUT)
                                    .invocationTimeout(HEALTH_CHECK_INVOCATION_TIMEOUT)
                                    .endpoint(HEALTH_CHECK_HTTP_ENDPOINT)
                                    .interval(interval)
                                    .build())
                          .build();
    }

    private static ReadinessHealthCheck buildReadinessHealthCheck() {
        return ReadinessHealthCheck.builder()
                                   .type(ReadinessHealthCheckType.PROCESS)
                                   .build();
    }

    private static RawCloudProcess buildRawProcess(HealthCheck healthCheck, ReadinessHealthCheck readinessHealthCheck) {
        ProcessResource processResource = ProcessResource.builder()
                                                         .id(PROCESS_ID)
                                                         .createdAt(CREATED_AT)
                                                         .type("web")
                                                         .command(COMMAND)
                                                         .instances(INSTANCES)
                                                         .memoryInMb(MEMORY_IN_MB)
                                                         .diskInMb(DISK_IN_MB)
                                                         .healthCheck(healthCheck)
                                                         .readinessHealthCheck(readinessHealthCheck)
                                                         .metadata(Metadata.builder()
                                                                           .build())
                                                         .relationships(ProcessRelationships.builder()
                                                                                            .app(ToOneRelationship.builder()
                                                                                                                  .data(Relationship.builder()
                                                                                                                                    .id("app-guid")
                                                                                                                                    .build())
                                                                                                                  .build())
                                                                                            .build())
                                                         .build();
        return ImmutableRawCloudProcess.of(processResource);
    }
}
