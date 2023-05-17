package org.cloudfoundry.multiapps.controller.core.helpers;

import java.util.Set;

import org.cloudfoundry.multiapps.controller.core.model.DynamicResolvableParameter;

public class DynamicResolvableParametersHelper {

    private final Set<DynamicResolvableParameter> dynamicResolvableParameters;

    public DynamicResolvableParametersHelper(Set<DynamicResolvableParameter> dynamicResolvableParameters) {
        this.dynamicResolvableParameters = dynamicResolvableParameters;
    }

    public DynamicResolvableParameter findDynamicResolvableParameter(String parameterName, String relationshipName) {
        for (var dynamicResolvableParameter : dynamicResolvableParameters) {
            if (doesParameterNameAndRelationshipMatch(dynamicResolvableParameter, parameterName, relationshipName)) {
                return dynamicResolvableParameter;
            }
        }
        return null;
    }

    private boolean doesParameterNameAndRelationshipMatch(DynamicResolvableParameter dynamicResolvableParameter, String parameterName,
                                                          String relationshipName) {
        return dynamicResolvableParameter.getParameterName()
                                         .equals(parameterName)
            && dynamicResolvableParameter.getRelationshipEntityName()
                                         .equals(relationshipName);
    }

}
