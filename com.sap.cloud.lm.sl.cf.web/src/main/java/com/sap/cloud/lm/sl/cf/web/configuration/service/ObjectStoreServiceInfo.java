package com.sap.cloud.lm.sl.cf.web.configuration.service;

import org.springframework.cloud.service.BaseServiceInfo;
import org.springframework.cloud.service.ServiceInfo.ServiceLabel;

@ServiceLabel("objectstore")
public class ObjectStoreServiceInfo extends BaseServiceInfo {

    private String provider;
    private String identity;
    private String credential;
    private String container;
    private String region;
    private String host;

    public ObjectStoreServiceInfo(String id, String provider, String identity, String credential, String container, String region, String host) {
        super(id);
        this.provider = provider;
        this.identity = identity;
        this.credential = credential;
        this.container = container;
        this.region = region;
        this.host = host;
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
    
    public String getRegion() {
        return region;
    }
    
    public String getHost() {
        return host;
    }
}
