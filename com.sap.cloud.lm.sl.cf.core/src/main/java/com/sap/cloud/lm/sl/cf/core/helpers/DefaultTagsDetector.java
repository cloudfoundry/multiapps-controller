package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.springframework.web.client.RestTemplate;

public class DefaultTagsDetector {

    @Inject
    protected RestTemplateFactory restTemplateFactory;

    public Map<String, List<String>> computeDefaultTags(CloudFoundryOperations client) {
        String servicesUrl = getServicesEndpoint(client.getCloudControllerUrl().toString());
        Resources resources = getRestTemplate(client).getForObject(servicesUrl, Resources.class);
        Map<String, List<String>> defaultTags = new HashMap<>();
        for (Resource service : resources.getResources()) {
            ServiceEntity entity = service.getEntity();
            defaultTags.put(entity.getLabel(), entity.getTags());
        }
        return defaultTags;
    }

    protected String getServicesEndpoint(String controllerUrl) {
        return controllerUrl + "/v2/services";
    }

    private RestTemplate getRestTemplate(CloudFoundryOperations client) {
        return restTemplateFactory.getRestTemplate(client);
    }

    protected static class Resources {

        protected List<Resource> resources;

        public List<Resource> getResources() {
            return resources;
        }

    }

    protected static class Resource {

        protected ServiceEntity entity;

        public ServiceEntity getEntity() {
            return entity;
        }

    }

    protected static class ServiceEntity {

        protected String label;
        protected List<String> tags;

        public String getLabel() {
            return label;
        }

        public List<String> getTags() {
            return tags;
        }

    }

}
