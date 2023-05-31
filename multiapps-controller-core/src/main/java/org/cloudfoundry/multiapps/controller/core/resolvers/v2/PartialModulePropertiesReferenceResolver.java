package org.cloudfoundry.multiapps.controller.core.resolvers.v2;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.resolvers.ResolverBuilder;
import org.cloudfoundry.multiapps.mta.resolvers.v2.ModulePropertiesReferenceResolver;

public class PartialModulePropertiesReferenceResolver extends ModulePropertiesReferenceResolver {

    private final List<String> dependenciesToIgnore;

    public PartialModulePropertiesReferenceResolver(DeploymentDescriptor descriptor, Module module, Map<String, Object> properties,
                                                    String prefix, List<String> dependenciesToIgnore,
                                                    Set<String> dynamicResolvableParameters) {
        super(descriptor, module, properties, prefix, new ResolverBuilder(), dynamicResolvableParameters);
        this.dependenciesToIgnore = dependenciesToIgnore;
    }

    @Override
    protected Map<String, Object> resolve(Map<String, Object> parameters) {
        return new PartialPropertiesResolver(parameters,
                                             this,
                                             patternToMatch,
                                             prefix,
                                             dependenciesToIgnore,
                                             dynamicResolvableParameters).resolve();
    }

}
