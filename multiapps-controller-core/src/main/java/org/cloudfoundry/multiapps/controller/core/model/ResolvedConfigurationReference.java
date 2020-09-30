package org.cloudfoundry.multiapps.controller.core.model;

import java.util.List;

import org.cloudfoundry.multiapps.controller.persistence.model.filters.ConfigurationFilter;
import org.cloudfoundry.multiapps.mta.model.Resource;

public class ResolvedConfigurationReference {

    private final ConfigurationFilter referenceFilter;
    private final Resource reference;
    private final List<Resource> resolvedResources;

    public ResolvedConfigurationReference(ConfigurationFilter referenceFilter, Resource reference, List<Resource> resolvedResources) {
        this.resolvedResources = resolvedResources;
        this.reference = reference;
        this.referenceFilter = referenceFilter;
    }

    public ConfigurationFilter getReferenceFilter() {
        return referenceFilter;
    }

    public Resource getReference() {
        return reference;
    }

    public List<Resource> getResolvedResources() {
        return resolvedResources;
    }

}
