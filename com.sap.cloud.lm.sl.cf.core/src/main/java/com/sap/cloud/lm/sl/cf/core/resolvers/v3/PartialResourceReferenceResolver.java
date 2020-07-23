package com.sap.cloud.lm.sl.cf.core.resolvers.v3;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.cloudfoundry.multiapps.mta.resolvers.ResolverBuilder;
import org.cloudfoundry.multiapps.mta.resolvers.v3.ResourcePropertiesReferenceResolver;
import org.cloudfoundry.multiapps.mta.resolvers.v3.ResourceReferenceResolver;

public class PartialResourceReferenceResolver extends ResourceReferenceResolver {

    private final List<String> dependenciesToIgnore;

    public PartialResourceReferenceResolver(DeploymentDescriptor descriptor, Resource resource, String prefix,
                                            List<String> dependenciesToIgnore) {
        super(descriptor, resource, prefix, new ResolverBuilder(), new ResolverBuilder());
        this.dependenciesToIgnore = dependenciesToIgnore;
    }

    @Override
    protected ResourcePropertiesReferenceResolver createResourcePropertiesReferenceResolver(Map<String, Object> properties) {
        return new PartialResourcePropertiesReferenceResolver(descriptor, resource, properties, prefix, dependenciesToIgnore);
    }

}
