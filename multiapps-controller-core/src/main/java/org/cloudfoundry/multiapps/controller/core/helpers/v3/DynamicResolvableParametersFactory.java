package org.cloudfoundry.multiapps.controller.core.helpers.v3;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.core.model.DynamicResolvableParameter;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDynamicResolvableParameter;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.cloudfoundry.multiapps.mta.util.DynamicParameterUtil;

public class DynamicResolvableParametersFactory
    extends org.cloudfoundry.multiapps.controller.core.helpers.v2.DynamicResolvableParametersFactory {

    public DynamicResolvableParametersFactory(DeploymentDescriptor descriptor) {
        super(descriptor);
    }

    @Override
    public Set<DynamicResolvableParameter> create() {
        Set<DynamicResolvableParameter> dynamicResolvableParameters = new HashSet<>();
        for (Resource resource : descriptor.getResources()) {
            addDynamicResolvableParameter(dynamicResolvableParameters, resource);
        }
        return dynamicResolvableParameters;
    }

    private void addDynamicResolvableParameter(Set<DynamicResolvableParameter> dynamicResolvableParameters, Resource resource) {
        List<String> parametersStringValues = getStringValueParameters(resource.getParameters());
        List<String> propertiesStringValues = getStringValueParameters(resource.getProperties());

        addDynamicResolvableParameter(dynamicResolvableParameters, parametersStringValues);
        addDynamicResolvableParameter(dynamicResolvableParameters, propertiesStringValues);
    }

    private List<String> getStringValueParameters(Map<String, Object> parameters) {
        return parameters.values()
                         .stream()
                         .filter(String.class::isInstance)
                         .map(String.class::cast)
                         .collect(Collectors.toList());
    }

    private void addDynamicResolvableParameter(Set<DynamicResolvableParameter> dynamicResolvableParameters,
                                               List<String> parametersStringValues) {
        for (String parameter : parametersStringValues) {
            if (parameter.matches(DynamicParameterUtil.REGEX_PATTERN_FOR_DYNAMIC_PARAMETERS)) {
                String relationshipName = DynamicParameterUtil.getRelationshipName(parameter);
                String parameterName = DynamicParameterUtil.getParameterName(parameter);
                dynamicResolvableParameters.add(ImmutableDynamicResolvableParameter.builder()
                                                                                   .relationshipEntityName(relationshipName)
                                                                                   .parameterName(parameterName)
                                                                                   .build());
            }
        }
    }

}
