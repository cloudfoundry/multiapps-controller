package com.sap.cloud.lm.sl.cf.core.resolvers.v3;

import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.mta.model.v3.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v3.Resource;
import com.sap.cloud.lm.sl.mta.resolvers.ResolverBuilder;
import com.sap.cloud.lm.sl.mta.resolvers.v3.ResourcePropertiesReferenceResolver;
import com.sap.cloud.lm.sl.mta.resolvers.v3.ResourceReferenceResolver;

public class PartialResourceReferenceResolver extends ResourceReferenceResolver {

    private List<String> dependenciesToIgnore;

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
