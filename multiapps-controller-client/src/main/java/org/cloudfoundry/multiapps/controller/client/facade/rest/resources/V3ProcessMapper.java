package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudProcess;
import org.cloudfoundry.multiapps.controller.client.facade.domain.HealthCheckType;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudProcess;

/**
 * Maps the {@link V3Process} wire model to the project's {@link CloudProcess} domain object. Mirrors the OSS
 * {@code RawCloudProcess} adapter field-for-field, so both client implementations yield identical domain objects.
 */
public final class V3ProcessMapper {

    private V3ProcessMapper() {
    }

    public static CloudProcess toCloudProcess(V3Process process) {
        V3Process.V3HealthCheck healthCheck = process.healthCheck();
        V3Process.V3HealthCheck readinessHealthCheck = process.readinessHealthCheck();

        Integer healthCheckTimeout = null;
        String healthCheckHttpEndpoint = null;
        Integer healthCheckInvocationTimeout = null;
        Integer healthCheckInterval = null;
        if (healthCheck != null && healthCheck.data() != null) {
            V3Process.V3HealthCheckData healthCheckData = healthCheck.data();
            healthCheckTimeout = healthCheckData.timeout();
            healthCheckInvocationTimeout = healthCheckData.invocationTimeout();
            healthCheckHttpEndpoint = healthCheckData.endpoint();
            healthCheckInterval = healthCheckData.interval();
        }

        Integer readinessHealthCheckInvocationTimeout = null;
        String readinessHealthCheckHttpEndpoint = null;
        Integer readinessHealthCheckInterval = null;
        if (readinessHealthCheck != null && readinessHealthCheck.data() != null) {
            V3Process.V3HealthCheckData readinessHealthCheckData = readinessHealthCheck.data();
            readinessHealthCheckInvocationTimeout = readinessHealthCheckData.invocationTimeout();
            readinessHealthCheckHttpEndpoint = readinessHealthCheckData.endpoint();
            readinessHealthCheckInterval = readinessHealthCheckData.interval();
        }

        return ImmutableCloudProcess.builder()
                                    .command(process.command())
                                    .instances(process.instances())
                                    .memoryInMb(process.memoryInMb())
                                    .diskInMb(process.diskInMb())
                                    .healthCheckType(HealthCheckType.valueOf(healthCheck.type()
                                                                                        .toUpperCase()))
                                    .healthCheckHttpEndpoint(healthCheckHttpEndpoint)
                                    .healthCheckTimeout(healthCheckTimeout)
                                    .healthCheckInvocationTimeout(healthCheckInvocationTimeout)
                                    .healthCheckInterval(healthCheckInterval)
                                    .readinessHealthCheckType(readinessHealthCheck == null ? null : readinessHealthCheck.type())
                                    .readinessHealthCheckHttpEndpoint(readinessHealthCheckHttpEndpoint)
                                    .readinessHealthCheckInvocationTimeout(readinessHealthCheckInvocationTimeout)
                                    .readinessHealthCheckInterval(readinessHealthCheckInterval)
                                    .build();
    }

}
