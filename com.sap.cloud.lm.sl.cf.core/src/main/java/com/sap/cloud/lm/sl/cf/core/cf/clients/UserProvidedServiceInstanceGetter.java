package com.sap.cloud.lm.sl.cf.core.cf.clients;

import org.springframework.stereotype.Component;

@Component("userProvidedServiceInstanceGetter")
public class UserProvidedServiceInstanceGetter extends AbstractServiceGetter {

    public UserProvidedServiceInstanceGetter(RestTemplateFactory restTemplateFactory) {
        super(restTemplateFactory);
    }

    private static final String SERVICE_INSTANCES_URL = "/v2/user_provided_service_instances?q=name:{name}&q=space_guid:{space_guid}";

    @Override
    public String getServiceInstanceURL() {
        return SERVICE_INSTANCES_URL;
    }

}
