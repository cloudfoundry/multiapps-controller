package com.sap.cloud.lm.sl.cf.client.lib.domain;

import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudInfo;

public class CloudInfoExtended extends CloudInfo {

    private final boolean portBasedRouting;
    private final String deployServiceUrl;

    public CloudInfoExtended(Map<String, Object> infoMap, boolean portBasedRouting, String deployServiceUrl) {
        super(infoMap);
        this.portBasedRouting = portBasedRouting;
        this.deployServiceUrl = deployServiceUrl;
    }

    public CloudInfoExtended(String name, String support, String authorizationEndpoint, String build, String version, String user,
        String description, Limits limits, Usage usage, boolean allowDebug, String loggregatorEndpoint, boolean portBasedRouting,
        String deployServiceUrl) {
        super(name, support, authorizationEndpoint, build, version, user, description, limits, usage, allowDebug, loggregatorEndpoint);
        this.portBasedRouting = portBasedRouting;
        this.deployServiceUrl = deployServiceUrl;
    }

    public boolean isPortBasedRouting() {
        return portBasedRouting;
    }

    public String getDeployServiceUrl() {
        return deployServiceUrl;
    }

    public String getServiceUrl(String serviceName) {
        return null;
    }

}
