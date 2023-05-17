package org.cloudfoundry.multiapps.controller.core.resolvers.v3;

import java.util.List;
import java.util.Set;

import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.cloudfoundry.multiapps.mta.resolvers.ResolverBuilder;
import org.cloudfoundry.multiapps.mta.resolvers.v3.DescriptorReferenceResolver;
import org.cloudfoundry.multiapps.mta.resolvers.v3.ModuleReferenceResolver;
import org.cloudfoundry.multiapps.mta.resolvers.v3.ResourceReferenceResolver;

public class PartialDescriptorReferenceResolver extends DescriptorReferenceResolver {

    private final List<String> dependenciesToIgnore;

    public PartialDescriptorReferenceResolver(DeploymentDescriptor descriptor, List<String> dependenciesToIgnore,
                                              Set<String> dynamicResolvableParameters) {
        super(descriptor, new ResolverBuilder(), new ResolverBuilder(), new ResolverBuilder(), dynamicResolvableParameters);
        this.dependenciesToIgnore = dependenciesToIgnore;
    }

    @Override
    protected ModuleReferenceResolver createModuleResolver(Module module) {
        return new PartialModuleReferenceResolver(descriptor, module, "", dependenciesToIgnore, dynamicResolvableParameters);
    }

    @Override
    protected ResourceReferenceResolver createResourceResolver(Resource resource) {
        return new PartialResourceReferenceResolver(descriptor, resource, "", dependenciesToIgnore, dynamicResolvableParameters);
    }

}
