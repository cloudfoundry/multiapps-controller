package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.Constants;
import com.sap.cloud.lm.sl.cf.core.util.V2UrlBuilder;

@Component("userProvidedServiceInstanceGetter")
@Profile("cf")
public class UserProvidedServiceInstanceGetter extends AbstractServiceGetter {

    public UserProvidedServiceInstanceGetter(RestTemplateFactory restTemplateFactory) {
        super(restTemplateFactory);
    }

    @Override
    protected String getResourcesName() {
        return Constants.SERVICE_INSTANCE_RESPONSE_RESOURCES;
    }

    @Override
    protected String getEntityName() {
        return Constants.SERVICE_INSTANCE_RESPONSE_ENTITY;
    }

    @Override
    public String getServiceInstanceURL(Set<String> fields) {
        return V2UrlBuilder.build(this::getUserProvidedServiceInstanceResourcePath, Constants.V2_QUERY_SEPARATOR, fields);
    }
}
