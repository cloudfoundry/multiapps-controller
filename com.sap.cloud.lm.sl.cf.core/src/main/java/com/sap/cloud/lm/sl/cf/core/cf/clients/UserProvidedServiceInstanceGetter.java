package com.sap.cloud.lm.sl.cf.core.cf.clients;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.Constants;

@Component("userProvidedServiceInstanceGetter")
@Profile("cf")
public class UserProvidedServiceInstanceGetter extends AbstractServiceGetter {

    public UserProvidedServiceInstanceGetter(RestTemplateFactory restTemplateFactory) {
        super(restTemplateFactory);
    }

    private static final String SERVICE_INSTANCES_URL = "/v2/user_provided_service_instances?q=name:{name}&q=space_guid:{space_guid}";

    @Override
    public String getServiceInstanceURL() {
        return SERVICE_INSTANCES_URL;
    }

    @Override
    protected Object getResourcesName() {
        return Constants.SERVICE_INSTANCE_RESPONSE_RESOURCES;
    }

    @Override
    protected Object getEntityName() {
        return Constants.SERVICE_INSTANCE_RESPONSE_ENTITY;
    }
}
