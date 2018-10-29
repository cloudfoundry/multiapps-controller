package com.sap.cloud.lm.sl.cf.core.util;

import java.util.Map;
import java.util.Set;

import com.sap.cloud.lm.sl.cf.core.activiti.Messages;
import com.sap.cloud.lm.sl.cf.core.cf.v1.ResourceType;
import com.sap.cloud.lm.sl.common.ContentException;

public class SpecialResourceTypesRequiredParametersUtil {

    public static void checkRequiredParameters(ResourceType resourceType, Map<String, Object> parameters) {
        Set<String> requiredParameters = resourceType.getRequiredParameters();
        for (String parameter : requiredParameters) {
            if (!parameters.containsKey(parameter)) {
                throw new ContentException(Messages.NO_VALUE_GIVEN_FOR_REQUIRED_PARAMETERS, parameter);
            }
        }
    }

}
