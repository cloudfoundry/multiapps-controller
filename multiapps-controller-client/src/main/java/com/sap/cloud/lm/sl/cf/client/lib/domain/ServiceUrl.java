package com.sap.cloud.lm.sl.cf.client.lib.domain;

public class ServiceUrl {

    private String serviceName;
    private String url;

    public ServiceUrl() {
    }

    public ServiceUrl(String serviceName, String url) {
        this.serviceName = serviceName;
        this.url = url;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
