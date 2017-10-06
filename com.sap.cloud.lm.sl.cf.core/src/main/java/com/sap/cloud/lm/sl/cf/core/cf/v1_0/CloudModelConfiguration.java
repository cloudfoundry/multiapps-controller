package com.sap.cloud.lm.sl.cf.core.cf.v1_0;

public class CloudModelConfiguration {
    protected boolean portBasedRouting;
    protected boolean prettyPrinting;
    protected boolean useNamespaces;
    protected boolean useNamespacesForServices;
    protected boolean allowInvalidEnvNames;

    public boolean isPortBasedRouting() {
        return portBasedRouting;
    }

    public void setPortBasedRouting(boolean portBasedRouting) {
        this.portBasedRouting = portBasedRouting;
    }

    public boolean isPrettyPrinting() {
        return prettyPrinting;
    }

    public void setPrettyPrinting(boolean prettyPrinting) {
        this.prettyPrinting = prettyPrinting;
    }

    public boolean shouldUseNamespaces() {
        return useNamespaces;
    }

    public void setUseNamespaces(boolean useNamespaces) {
        this.useNamespaces = useNamespaces;
    }

    public boolean shouldUseNamespacesForServices() {
        return useNamespacesForServices;
    }

    public void setUseNamespacesForServices(boolean useNamespacesForServices) {
        this.useNamespacesForServices = useNamespacesForServices;
    }

    public boolean shouldAllowInvalidEnvNames() {
        return allowInvalidEnvNames;
    }

    public void setAllowInvalidEnvNames(boolean allowInvalidEnvNames) {
        this.allowInvalidEnvNames = allowInvalidEnvNames;
    }


}
