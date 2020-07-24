package com.sap.cloud.lm.sl.cf.core.resolvers.v3;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.cloudfoundry.multiapps.mta.resolvers.ResolverBuilder;
import org.cloudfoundry.multiapps.mta.resolvers.v3.ResourcePropertiesReferenceResolver;

import com.sap.cloud.lm.sl.cf.core.resolvers.v2.PartialPropertiesResolver;

public class PartialResourcePropertiesReferenceResolver extends ResourcePropertiesReferenceResolver {

    private final List<String> dependenciesToIgnore;

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
