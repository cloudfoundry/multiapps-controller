package com.sap.cloud.lm.sl.cf.core.model;

import java.util.List;

import com.sap.cloud.lm.sl.mta.model.Resource;

public class ResolvedConfigurationReference {

    private ConfigurationFilter referenceFilter;
    private Resource reference;
    private List<Resource> resolvedResources;

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
