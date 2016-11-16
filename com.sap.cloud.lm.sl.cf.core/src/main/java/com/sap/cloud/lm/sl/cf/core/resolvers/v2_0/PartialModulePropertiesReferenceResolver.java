package com.sap.cloud.lm.sl.cf.core.resolvers.v2_0;

import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.mta.model.v2_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2_0.Module;
import com.sap.cloud.lm.sl.mta.resolvers.v2_0.ModulePropertiesReferenceResolver;

public class PartialModulePropertiesReferenceResolver extends ModulePropertiesReferenceResolver {

    private List<String> dependenciesToIgnore;

    public PartialModulePropertiesReferenceResolver(DeploymentDescriptor descriptor, Module module, Map<String, Object> properties,
        String prefix, List<String> dependenciesToIgnore) {
        super(descriptor, module, properties, prefix);
        this.dependenciesToIgnore = dependenciesToIgnore;
    }

    @Override
    protected Map<String, Object> resolve(Map<String, Object> parameters) throws ContentException {
        return new PartialPropertiesResolver(parameters, this, patternToMatch, prefix, dependenciesToIgnore).resolve();
    }

}
