package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.util.List;
import java.util.Map;

public class CloudResourcesWithIncluded {

    private final List<Map<String, Object>> resources;
    private final Map<String, Object> included;

    private CloudResourcesWithIncluded(List<Map<String, Object>> resources, Map<String, Object> included) {
        this.resources = resources;
        this.included = included;
    }

    public static CloudResourcesWithIncluded of(List<Map<String, Object>> resources, Map<String, Object> included) {
        return new CloudResourcesWithIncluded(resources, included);
    }

    public List<Map<String, Object>> getResources() {
        return resources;
    }

    public Object getIncludedResource(String resource) {
        return included.get(resource);
    }
}
