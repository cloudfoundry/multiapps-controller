package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import org.cloudfoundry.client.v3.processes.Data;
import org.cloudfoundry.client.v3.processes.HealthCheck;
import org.cloudfoundry.client.v3.processes.Process;
import org.immutables.value.Value;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudProcess;
import org.cloudfoundry.multiapps.controller.client.facade.domain.HealthCheckType;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudProcess;

@Value.Immutable
public abstract class RawCloudProcess extends RawCloudEntity<CloudProcess> {

    @Value.Parameter
    public abstract Process getProcess();

    @Override
    public CloudProcess derive() {
        Process process = getProcess();
        HealthCheck healthCheck = process.getHealthCheck();
        Integer healthCheckTimeout = null;
        String healthCheckHttpEndpoint = null;
        Integer healthCheckInvocationTimeout = null;
        if (healthCheck.getData() != null) {
            Data healthCheckData = healthCheck.getData();
            healthCheckTimeout = healthCheckData.getTimeout();
            healthCheckInvocationTimeout = healthCheckData.getInvocationTimeout();
            healthCheckHttpEndpoint = healthCheckData.getEndpoint();
        }
        return ImmutableCloudProcess.builder()
                                    .command(process.getCommand())
                                    .instances(process.getInstances())
                                    .memoryInMb(process.getMemoryInMb())
                                    .diskInMb(process.getDiskInMb())
                                    .healthCheckType(HealthCheckType.valueOf(healthCheck.getType()
                                                                                        .getValue()
                                                                                        .toUpperCase()))
                                    .healthCheckHttpEndpoint(healthCheckHttpEndpoint)
                                    .healthCheckTimeout(healthCheckTimeout)
                                    .healthCheckInvocationTimeout(healthCheckInvocationTimeout)
                                    .build();
    }
}
