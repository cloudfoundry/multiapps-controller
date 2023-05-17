package org.cloudfoundry.multiapps.controller.core.resolvers.v3;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.multiapps.controller.core.resolvers.v2.PartialModulePropertiesReferenceResolver;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.RequiredDependency;
import org.cloudfoundry.multiapps.mta.resolvers.ResolverBuilder;
import org.cloudfoundry.multiapps.mta.resolvers.v2.ModulePropertiesReferenceResolver;
import org.cloudfoundry.multiapps.mta.resolvers.v3.ModuleReferenceResolver;

public class PartialModuleReferenceResolver extends ModuleReferenceResolver {

    private final List<String> dependenciesToIgnore;

    public PartialModuleReferenceResolver(DeploymentDescriptor descriptor, Module module, String prefix, List<String> dependenciesToIgnore,
                                          Set<String> dynamicResolvableParameters) {
        super(descriptor, module, prefix, new ResolverBuilder(), new ResolverBuilder(), dynamicResolvableParameters);
        this.dependenciesToIgnore = dependenciesToIgnore;
    }

    @Override
    protected ModulePropertiesReferenceResolver createModulePropertiesReferenceResolver(Map<String, Object> properties) {
        return new PartialModulePropertiesReferenceResolver(descriptor,
                                                            module,
                                                            properties,
                                                            prefix,
                                                            dependenciesToIgnore,
                                                            dynamicResolvableParameters);
    }

    @Override
    protected RequiredDependency resolveRequiredDependency(RequiredDependency dependency) {
        if (!dependenciesToIgnore.contains(dependency.getName())) {
            return createRequiredDependencyResolver(dependency).resolve();
        }
        return dependency;
    }

}
