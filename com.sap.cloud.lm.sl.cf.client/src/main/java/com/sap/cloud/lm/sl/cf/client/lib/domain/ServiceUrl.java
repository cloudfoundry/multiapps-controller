package com.sap.cloud.lm.sl.cf.client.lib.domain;

public class ServiceUrl {

    private String serviceName;
    private String url;

    // Required by Jackson.
    public ServiceUrl() {
    }

    public ServiceUrl(String serviceName, String url) {
        this.serviceName = serviceName;
        this.url = url;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getUrl() {
        return url;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
