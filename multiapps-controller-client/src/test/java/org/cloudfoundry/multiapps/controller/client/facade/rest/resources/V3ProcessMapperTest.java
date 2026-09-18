package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudProcess;
import org.cloudfoundry.multiapps.controller.client.facade.domain.HealthCheckType;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Process.V3HealthCheck;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Process.V3HealthCheckData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class V3ProcessMapperTest {

    private static final String GUID_STRING = "3725a721-6e3f-4b6e-8f2b-1c2d3e4f5a6b";

    @Test
    void testToCloudProcessMapsAllFields() {
        V3HealthCheckData healthCheckData = new V3HealthCheckData(60, 10, "/health", 5);
        V3HealthCheckData readinessData = new V3HealthCheckData(null, 8, "/ready", 3);
        V3Process process = new V3Process(GUID_STRING, "start.sh", 3, 512, 2048, new V3HealthCheck("http", healthCheckData),
                                          new V3HealthCheck("http", readinessData));

        CloudProcess result = V3ProcessMapper.toCloudProcess(process);

        Assertions.assertEquals("start.sh", result.getCommand());
        Assertions.assertEquals(3, result.getInstances());
        Assertions.assertEquals(HealthCheckType.HTTP, result.getHealthCheckType());
        Assertions.assertEquals("/health", result.getHealthCheckHttpEndpoint());
        Assertions.assertEquals(60, result.getHealthCheckTimeout());
        Assertions.assertEquals("http", result.getReadinessHealthCheckType());
        Assertions.assertEquals("/ready", result.getReadinessHealthCheckHttpEndpoint());
    }

    @Test
    void testToCloudProcessNullHealthCheckDefaultsToPort() {
        V3Process process = new V3Process(GUID_STRING, "start.sh", 1, 256, 1024, null, null);

        CloudProcess result = V3ProcessMapper.toCloudProcess(process);

        Assertions.assertEquals(HealthCheckType.PORT, result.getHealthCheckType());
        Assertions.assertNull(result.getReadinessHealthCheckType());
    }

    @Test
    void testToCloudProcessHealthCheckTypeIsUpperCased() {
        V3Process process = new V3Process(GUID_STRING, "s", 1, 256, 1024, new V3HealthCheck("process", null), null);

        Assertions.assertEquals(HealthCheckType.PROCESS, V3ProcessMapper.toCloudProcess(process)
                                                                        .getHealthCheckType());
    }

    @Test
    void testToCloudProcessNullHealthCheckDataLeavesEndpointFieldsNull() {
        V3Process process = new V3Process(GUID_STRING, "s", 1, 256, 1024, new V3HealthCheck("port", null), null);

        CloudProcess result = V3ProcessMapper.toCloudProcess(process);

        Assertions.assertEquals(HealthCheckType.PORT, result.getHealthCheckType());
        Assertions.assertNull(result.getHealthCheckHttpEndpoint());
        Assertions.assertNull(result.getHealthCheckTimeout());
    }

}
