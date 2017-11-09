package com.sap.cloud.lm.sl.cf.client.lib.domain;

import org.cloudfoundry.client.lib.domain.Staging;

public class StagingExtended extends Staging {

    private String healthCheckType;
    private String healthCheckHttpEndpoint;
    private Boolean sshEnabled;

    public StagingExtended(String command, String buildpackUrl, String stack, Integer healthCheckTimeout, String healthCheckType,
        String healthCheckHttpEndpoint, Boolean sshEnabled) {
        super(command, buildpackUrl, stack, healthCheckTimeout);
        this.healthCheckType = healthCheckType;
        this.healthCheckHttpEndpoint = healthCheckHttpEndpoint;
        this.sshEnabled = sshEnabled;
    }

    public StagingExtended(String command, String buildpackUrl, String stack, Integer healthCheckTimeout, String detectedBuildpack,
        String healthCheckType, String healthCheckHttpEndpoint, Boolean sshEnabled) {
        super(command, buildpackUrl, stack, healthCheckTimeout, detectedBuildpack);
        this.healthCheckType = healthCheckType;
        this.healthCheckHttpEndpoint = healthCheckHttpEndpoint;
        this.sshEnabled = sshEnabled;
    }

    public String getHealthCheckType() {
        return healthCheckType;
    }

    public String getHealthCheckHttpEndpoint() {
        return healthCheckHttpEndpoint;
    }

    public Boolean isSshEnabled() {
        return sshEnabled;
    }

}