package com.sap.cloud.lm.sl.cf.core.resolvers.v3;

import java.util.List;

import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.cloudfoundry.multiapps.mta.resolvers.ResolverBuilder;
import org.cloudfoundry.multiapps.mta.resolvers.v3.DescriptorReferenceResolver;
import org.cloudfoundry.multiapps.mta.resolvers.v3.ModuleReferenceResolver;
import org.cloudfoundry.multiapps.mta.resolvers.v3.ResourceReferenceResolver;

public class PartialDescriptorReferenceResolver extends DescriptorReferenceResolver {

    private final List<String> dependenciesToIgnore;

    public PartialDescriptorReferenceResolver(DeploymentDescriptor descriptor, List<String> dependenciesToIgnore) {
        super(descriptor, new ResolverBuilder(), new ResolverBuilder(), new ResolverBuilder());
        this.dependenciesToIgnore = dependenciesToIgnore;
    }

    @Override
    protected ModuleReferenceResolver createModuleResolver(Module module) {
        return new PartialModuleReferenceResolver(descriptor, module, "", dependenciesToIgnore);
    }

    @Override
    protected ResourceReferenceResolver createResourceResolver(Resource resource) {
        return new PartialResourceReferenceResolver(descriptor, resource, "", dependenciesToIgnore);
    }

}
