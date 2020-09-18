package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.util.Set;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.Constants;
import org.cloudfoundry.multiapps.controller.core.util.V2UrlBuilder;

@Named("serviceInstanceGetter")
public class ServiceInstanceGetter extends AbstractServiceGetter {

    protected ServiceInstanceGetter(WebClientFactory webClientFactory) {
        super(webClientFactory);
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
        return V2UrlBuilder.build(this::getServiceInstanceResourcePath, Constants.V2_QUERY_SEPARATOR, fields);
    }

}
