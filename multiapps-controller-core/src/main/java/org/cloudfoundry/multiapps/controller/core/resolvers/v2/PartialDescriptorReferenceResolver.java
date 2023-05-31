package org.cloudfoundry.multiapps.controller.core.resolvers.v2;

import java.util.List;
import java.util.Set;

import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.resolvers.ResolverBuilder;
import org.cloudfoundry.multiapps.mta.resolvers.v2.DescriptorReferenceResolver;
import org.cloudfoundry.multiapps.mta.resolvers.v2.ModuleReferenceResolver;

public class PartialDescriptorReferenceResolver extends DescriptorReferenceResolver {

    private final List<String> dependenciesToIgnore;

    public PartialDescriptorReferenceResolver(DeploymentDescriptor descriptor, List<String> dependenciesToIgnore,
                                              Set<String> dynamicResolvableParameters) {
        super(descriptor, new ResolverBuilder(), new ResolverBuilder(), dynamicResolvableParameters);
        this.dependenciesToIgnore = dependenciesToIgnore;
    }

    @Override
    protected ModuleReferenceResolver createModuleResolver(Module module) {
        return new PartialModuleReferenceResolver(descriptor, module, "", dependenciesToIgnore, dynamicResolvableParameters);
    }

}
