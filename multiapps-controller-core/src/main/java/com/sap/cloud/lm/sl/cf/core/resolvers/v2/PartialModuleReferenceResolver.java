package com.sap.cloud.lm.sl.cf.core.resolvers.v2;

import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.Module;
import com.sap.cloud.lm.sl.mta.model.v2.RequiredDependency;
import com.sap.cloud.lm.sl.mta.resolvers.ResolverBuilder;
import com.sap.cloud.lm.sl.mta.resolvers.v2.ModulePropertiesReferenceResolver;
import com.sap.cloud.lm.sl.mta.resolvers.v2.ModuleReferenceResolver;

public class PartialModuleReferenceResolver extends ModuleReferenceResolver {

    private List<String> dependenciesToIgnore;

    public PartialModuleReferenceResolver(DeploymentDescriptor descriptor, Module module, String prefix,
                                          List<String> dependenciesToIgnore) {
        super(descriptor, module, prefix, new ResolverBuilder(), new ResolverBuilder());
        this.dependenciesToIgnore = dependenciesToIgnore;
    }

    @Override
    protected ModulePropertiesReferenceResolver createModulePropertiesReferenceResolver(Map<String, Object> properties) {
        return new PartialModulePropertiesReferenceResolver((DeploymentDescriptor) descriptor,
                                                            module,
                                                            properties,
                                                            prefix,
                                                            dependenciesToIgnore);
    }

    @Override
    protected RequiredDependency resolveRequiredDependency(RequiredDependency dependency) {
        if (!dependenciesToIgnore.contains(dependency.getName())) {
            return createRequiredDependencyResolver(dependency).resolve();
        }
        return dependency;
    }

}
