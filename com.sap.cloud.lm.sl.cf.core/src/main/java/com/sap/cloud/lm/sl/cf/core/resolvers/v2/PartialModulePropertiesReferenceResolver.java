package com.sap.cloud.lm.sl.cf.core.resolvers.v2;

import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Module;
import com.sap.cloud.lm.sl.mta.resolvers.ResolverBuilder;
import com.sap.cloud.lm.sl.mta.resolvers.v2.ModulePropertiesReferenceResolver;

public class PartialModulePropertiesReferenceResolver extends ModulePropertiesReferenceResolver {

    private final List<String> dependenciesToIgnore;

    public PartialModulePropertiesReferenceResolver(DeploymentDescriptor descriptor, Module module, Map<String, Object> properties,
                                                    String prefix, List<String> dependenciesToIgnore) {
        super(descriptor, module, properties, prefix, new ResolverBuilder());
        this.dependenciesToIgnore = dependenciesToIgnore;
    }

    @Override
    protected Map<String, Object> resolve(Map<String, Object> parameters) {
        return new PartialPropertiesResolver(parameters, this, patternToMatch, prefix, dependenciesToIgnore).resolve();
    }

}
