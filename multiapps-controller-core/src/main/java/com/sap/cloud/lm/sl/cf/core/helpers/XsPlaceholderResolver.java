package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.List;
import java.util.stream.Collectors;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;

//TODO we should move this out of the cf. project
public class XsPlaceholderResolver {

    private String authorizationEndpoint;
    private String deployServiceUrl;
    private int routerPort;
    private String controllerEndpoint;
    private String defaultDomain;
    private String protocol;

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public void setAuthorizationEndpoint(String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    public String getDeployServiceUrl() {
        return deployServiceUrl;
    }

    public void setDeployServiceUrl(String deployServiceUrl) {
        this.deployServiceUrl = deployServiceUrl;
    }

    public int getRouterPort() {
        return routerPort;
    }

    public void setRouterPort(int routerPort) {
        this.routerPort = routerPort;
    }

    public String getControllerEndpoint() {
        return controllerEndpoint;
    }

    public void setControllerEndpoint(String controllerEndpoint) {
        this.controllerEndpoint = controllerEndpoint;
    }

    public String getDefaultDomain() {
        return defaultDomain;
    }

    public void setDefaultDomain(String defaultDomain) {
        this.defaultDomain = defaultDomain;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String resolve(String value) {
        value = replaceIfReplacementValueIsSet(value, SupportedParameters.XSA_CONTROLLER_ENDPOINT_PLACEHOLDER, controllerEndpoint);
        value = replaceIfReplacementValueIsSet(value, SupportedParameters.XSA_ROUTER_PORT_PLACEHOLDER, Integer.toString(routerPort));
        value = replaceIfReplacementValueIsSet(value, SupportedParameters.XSA_DEFAULT_DOMAIN_PLACEHOLDER, defaultDomain);
        value = replaceIfReplacementValueIsSet(value, SupportedParameters.XSA_AUTHORIZATION_ENDPOINT_PLACEHOLDER, authorizationEndpoint);
        value = replaceIfReplacementValueIsSet(value, SupportedParameters.XSA_PROTOCOL_PLACEHOLDER, protocol);
        value = replaceIfReplacementValueIsSet(value, SupportedParameters.XSA_DEPLOY_SERVICE_URL_PLACEHOLDER, deployServiceUrl);
        return value;
    }

    public List<String> resolve(List<String> values) {
        return values.stream()
                     .map(this::resolve)
                     .collect(Collectors.toList());
    }

    private String replaceIfReplacementValueIsSet(String value, String placeholder, String replacementValue) {
        if (replacementValue != null && value != null) {
            return value.replace(placeholder, replacementValue);
        }
        return value;
    }

}
