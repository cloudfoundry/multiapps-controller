package com.sap.cloud.lm.sl.cf.core.cf.v2;

import com.sap.cloud.lm.sl.mta.model.v2.Resource;

public class ResourceAndResourceType {
    private final Resource resource;
    private final ResourceType resourceType;

    public ResourceAndResourceType(Resource resource, ResourceType resourceType) {
        this.resource = resource;
        this.resourceType = resourceType;
    }

    public Resource getResource() {
        return resource;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }
}
