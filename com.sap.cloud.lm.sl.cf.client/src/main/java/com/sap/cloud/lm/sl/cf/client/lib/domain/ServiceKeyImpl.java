package com.sap.cloud.lm.sl.cf.client.lib.domain;

import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudService;

public class ServiceKeyImpl implements ServiceKey {
    private String name;
    private Map<String, Object> parameters;
    private Map<String, Object> credentials;
    private String guid;
    private CloudService service;
    
    public ServiceKeyImpl() {
    }
    
    public ServiceKeyImpl(String name, Map<String, Object> parameters, Map<String, Object> credentials, String guid, CloudService service) {
        this.name = name;
        this.parameters = parameters;
        this.credentials = credentials;
        this.guid = guid;
        this.service = service;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Override
    public Map<String, Object> getCredentials() {
        return credentials;
    }

    @Override
    public String getGuid() {
        return guid;
    }

    @Override
    public CloudService getService() {
        return service;
    }

}
