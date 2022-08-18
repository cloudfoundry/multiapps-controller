package org.cloudfoundry.multiapps.controller.client.lib.domain;

import com.sap.cloudfoundry.client.facade.domain.CloudProcess;
import com.sap.cloudfoundry.client.facade.domain.Staging;

import java.util.Objects;

public class HealthCheckInfo {

    private final String type;
    private final Integer timeout;
    private final Integer invocationTimeout;
    private final String httpEndpoint;

    private HealthCheckInfo(String type, Integer timeout, Integer invocationTimeout, String httpEndpoint) {
        this.type = type;
        this.timeout = timeout;
        this.invocationTimeout = invocationTimeout;
        this.httpEndpoint = httpEndpoint;
    }

    public static HealthCheckInfo fromStaging(Staging staging) {
        var type = staging.getHealthCheckType();
        if (type == null) {
            type = "port"; //use default health check type https://v3-apidocs.cloudfoundry.org/version/3.119.0/#the-health-check-object
        }
        var timeout = staging.getHealthCheckTimeout();
        var invocationTimeout = staging.getInvocationTimeout();
        var httpEndpoint = staging.getHealthCheckHttpEndpoint();
        return new HealthCheckInfo(type, timeout, invocationTimeout, httpEndpoint);
    }

    public static HealthCheckInfo fromProcess(CloudProcess process) {
        var type = process.getHealthCheckType();
        var timeout = process.getHealthCheckTimeout();
        var invocationTimeout = process.getHealthCheckInvocationTimeout();
        var httpEndpoint = process.getHealthCheckHttpEndpoint();
        return new HealthCheckInfo(type.toString(), timeout, invocationTimeout, httpEndpoint);
    }

    public String getType() {
        return type;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public Integer getInvocationTimeout() {
        return invocationTimeout;
    }

    public String getHttpEndpoint() {
        return httpEndpoint;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof HealthCheckInfo)) {
            return false;
        }
        var other = (HealthCheckInfo) obj;
        return Objects.equals(getType(), other.getType())
            && Objects.equals(getTimeout(), other.getTimeout())
            && Objects.equals(getInvocationTimeout(), other.getInvocationTimeout())
            && Objects.equals(getHttpEndpoint(), other.getHttpEndpoint());
    }
}
