package com.sap.cloud.lm.sl.cf.client.lib.domain;

import org.cloudfoundry.client.lib.domain.Staging;

public class StagingExtended extends Staging {

    private String healthCheckType;
    private String healthCheckHttpEndpoint;

    public StagingExtended(String command, String buildpackUrl, String stack, Integer healthCheckTimeout, String healthCheckType,
        String healthCheckHttpEndpoint) {
        super(command, buildpackUrl, stack, healthCheckTimeout);
        this.healthCheckType = healthCheckType;
        this.healthCheckHttpEndpoint = healthCheckHttpEndpoint;
    }

    public StagingExtended(String command, String buildpackUrl, String stack, Integer healthCheckTimeout, String detectedBuildpack,
        String healthCheckType, String healthCheckHttpEndpoint) {
        super(command, buildpackUrl, stack, healthCheckTimeout, detectedBuildpack);
        this.healthCheckType = healthCheckType;
        this.healthCheckHttpEndpoint = healthCheckHttpEndpoint;
    }

    public String getHealthCheckType() {
        return healthCheckType;
    }

    public String getHealthCheckHttpEndpoint() {
        return healthCheckHttpEndpoint;
    }

}