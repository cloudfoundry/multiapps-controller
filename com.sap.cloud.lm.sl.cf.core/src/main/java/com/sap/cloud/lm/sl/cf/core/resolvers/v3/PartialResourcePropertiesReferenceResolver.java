package com.sap.cloud.lm.sl.cf.core.resolvers.v3;

import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.resolvers.v2.PartialPropertiesResolver;
import com.sap.cloud.lm.sl.mta.model.v3.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v3.Resource;
import com.sap.cloud.lm.sl.mta.resolvers.ResolverBuilder;
import com.sap.cloud.lm.sl.mta.resolvers.v3.ResourcePropertiesReferenceResolver;

public class PartialResourcePropertiesReferenceResolver extends ResourcePropertiesReferenceResolver {

    private List<String> dependenciesToIgnore;

    public PartialResourcePropertiesReferenceResolver(DeploymentDescriptor descriptor, Resource resource, Map<String, Object> properties,
        String prefix, List<String> dependenciesToIgnore) {
        super(descriptor, resource, properties, prefix, new ResolverBuilder());
        this.dependenciesToIgnore = dependenciesToIgnore;
    }

    @Override
    public Map<String, Object> resolve() {
        return new PartialPropertiesResolver(properties, this, patternToMatch, prefix, dependenciesToIgnore).resolve();
    }

}
