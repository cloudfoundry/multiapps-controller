package org.cloudfoundry.multiapps.controller.core.cf.v2;

import org.cloudfoundry.multiapps.mta.model.Resource;

public class ResourceAndResourceType {
    private final Resource resource;
    private final ResourceType resourceType;

    public Resource getResource() {
        return resource;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public ResourceAndResourceType(Resource resource, ResourceType resourceType) {
        this.resource = resource;
        this.resourceType = resourceType;
    }
}
