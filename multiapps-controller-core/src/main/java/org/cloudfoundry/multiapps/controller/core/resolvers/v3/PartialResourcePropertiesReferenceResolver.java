package org.cloudfoundry.multiapps.controller.core.resolvers.v3;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.multiapps.controller.core.resolvers.v2.PartialPropertiesResolver;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.cloudfoundry.multiapps.mta.resolvers.ResolverBuilder;
import org.cloudfoundry.multiapps.mta.resolvers.v3.ResourcePropertiesReferenceResolver;

public class PartialResourcePropertiesReferenceResolver extends ResourcePropertiesReferenceResolver {

    private final List<String> dependenciesToIgnore;

    public PartialResourcePropertiesReferenceResolver(DeploymentDescriptor descriptor, Resource resource, Map<String, Object> properties,
                                                      String prefix, List<String> dependenciesToIgnore,
                                                      Set<String> dynamicResolvableParameters) {
        super(descriptor, resource, properties, prefix, new ResolverBuilder(), dynamicResolvableParameters);
        this.dependenciesToIgnore = dependenciesToIgnore;
    }

    @Override
    public Map<String, Object> resolve() {
        return new PartialPropertiesResolver(properties,
                                             this,
                                             patternToMatch,
                                             prefix,
                                             dependenciesToIgnore,
                                             dynamicResolvableParameters).resolve();
    }

}
