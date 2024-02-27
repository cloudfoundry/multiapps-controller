package com.sap.cloud.lm.sl.cf.core.resolvers.v3;

import java.util.List;

import com.sap.cloud.lm.sl.cf.core.resolvers.v2.PartialModuleReferenceResolver;
import com.sap.cloud.lm.sl.mta.model.v2.Module;
import com.sap.cloud.lm.sl.mta.model.v3.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v3.Resource;
import com.sap.cloud.lm.sl.mta.resolvers.ResolverBuilder;
import com.sap.cloud.lm.sl.mta.resolvers.v2.ModuleReferenceResolver;
import com.sap.cloud.lm.sl.mta.resolvers.v3.DescriptorReferenceResolver;
import com.sap.cloud.lm.sl.mta.resolvers.v3.ResourceReferenceResolver;

public class PartialDescriptorReferenceResolver extends DescriptorReferenceResolver {

    private List<String> dependenciesToIgnore;

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
        return new PartialResourceReferenceResolver((DeploymentDescriptor) descriptor, resource, "", dependenciesToIgnore);
    }

}
