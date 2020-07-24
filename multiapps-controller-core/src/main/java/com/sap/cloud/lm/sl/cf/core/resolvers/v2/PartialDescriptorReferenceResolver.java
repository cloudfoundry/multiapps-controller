package com.sap.cloud.lm.sl.cf.core.resolvers.v2;

import java.util.List;

import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.resolvers.ResolverBuilder;
import org.cloudfoundry.multiapps.mta.resolvers.v2.DescriptorReferenceResolver;
import org.cloudfoundry.multiapps.mta.resolvers.v2.ModuleReferenceResolver;

public class PartialDescriptorReferenceResolver extends DescriptorReferenceResolver {

    private final List<String> dependenciesToIgnore;

    public PartialDescriptorReferenceResolver(DeploymentDescriptor descriptor, List<String> dependenciesToIgnore) {
        super(descriptor, new ResolverBuilder(), new ResolverBuilder());
        this.dependenciesToIgnore = dependenciesToIgnore;
    }

    @Override
    protected ModuleReferenceResolver createModuleResolver(Module module) {
        return new PartialModuleReferenceResolver(descriptor, module, "", dependenciesToIgnore);
    }

}
