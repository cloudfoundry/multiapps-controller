package com.sap.cloud.lm.sl.cf.client.lib.domain;

import org.cloudfoundry.client.lib.domain.Staging;

public class StagingExtended extends Staging {

    private String healthCheckType;

    public StagingExtended(String command, String buildpackUrl, String stack, Integer healthCheckTimeout, String healthCheckType) {
        super(command, buildpackUrl, stack, healthCheckTimeout);
        this.healthCheckType = healthCheckType;
    }

    public StagingExtended(String command, String buildpackUrl, String stack, Integer healthCheckTimeout, String
        detectedBuildpack, String healthCheckType){
        super(command, buildpackUrl, stack, healthCheckTimeout, detectedBuildpack);
        this.healthCheckType = healthCheckType;
    }

    public String getHealthCheckType() {
        return healthCheckType;
    }
}