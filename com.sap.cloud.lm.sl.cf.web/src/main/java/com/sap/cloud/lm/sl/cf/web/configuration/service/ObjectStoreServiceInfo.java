package com.sap.cloud.lm.sl.cf.web.configuration.service;

import org.springframework.cloud.service.BaseServiceInfo;
import org.springframework.cloud.service.ServiceInfo.ServiceLabel;

@ServiceLabel("objectstore")
public class ObjectStoreServiceInfo extends BaseServiceInfo {

    private String provider;
    private String identity;
    private String credential;
    private String container;

    public ObjectStoreServiceInfo(String id, String provider, String identity, String credential, String container) {
        super(id);
        this.provider = provider;
        this.identity = identity;
        this.credential = credential;
        this.container = container;
    }

    public String getProvider() {
        return provider;
    }

    public String getIdentity() {
        return identity;
    }

    public String getCredential() {
        return credential;
    }

    public String getContainer() {
        return container;
    }
}
