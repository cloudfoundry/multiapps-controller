package com.sap.cloud.lm.sl.cf.client.lib.domain;

public class ServiceKeyToInject {
    private String envVarName;
    private String serviceName;
    private String serviceKeyName;

    // Required by Jackson.
    public ServiceKeyToInject() {
    }

    public ServiceKeyToInject(String envVarName, String serviceName, String serviceKeyName) {
        this.envVarName = envVarName;
        this.serviceName = serviceName;
        this.serviceKeyName = serviceKeyName;
    }

    public String getEnvVarName() {
        return envVarName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceKeyName() {
        return serviceKeyName;
    }

}
