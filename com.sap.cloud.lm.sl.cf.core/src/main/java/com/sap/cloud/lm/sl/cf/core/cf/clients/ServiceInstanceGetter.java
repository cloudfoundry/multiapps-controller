package com.sap.cloud.lm.sl.cf.core.cf.clients;

import org.springframework.stereotype.Component;

@Component("serviceInstanceGetter")
public class ServiceInstanceGetter extends AbstractServiceGetter {

    protected ServiceInstanceGetter(RestTemplateFactory restTemplateFactory) {
        super(restTemplateFactory);
    }

    private static final String SERVICE_INSTANCES_URL = "/v2/service_instances?q=name:{name}&q=space_guid:{space_guid}";

    @Override
    public String getServiceInstanceURL() {
        return SERVICE_INSTANCES_URL;
    }

}
