package com.sap.cloud.lm.sl.cf.core.resolvers.v2_0;

import java.util.List;

import com.sap.cloud.lm.sl.mta.model.v2_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2_0.Module;
import com.sap.cloud.lm.sl.mta.resolvers.ResolverBuilder;
import com.sap.cloud.lm.sl.mta.resolvers.v2_0.DescriptorReferenceResolver;
import com.sap.cloud.lm.sl.mta.resolvers.v2_0.ModuleReferenceResolver;

public class PartialDescriptorReferenceResolver extends DescriptorReferenceResolver {

    private List<String> dependenciesToIgnore;

    public PartialDescriptorReferenceResolver(DeploymentDescriptor descriptor, List<String> dependenciesToIgnore) {
        super(descriptor, new ResolverBuilder(), new ResolverBuilder());
        this.dependenciesToIgnore = dependenciesToIgnore;
    }

    @Override
    protected ModuleReferenceResolver createModuleResolver(Module module) {
        return new PartialModuleReferenceResolver(descriptor, module, "", dependenciesToIgnore);
    }

}
