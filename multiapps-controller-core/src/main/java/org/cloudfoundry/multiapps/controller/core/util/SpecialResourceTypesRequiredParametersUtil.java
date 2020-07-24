package org.cloudfoundry.multiapps.controller.core.util;

import java.util.Map;
import java.util.Set;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ResourceType;

public class SpecialResourceTypesRequiredParametersUtil {

    private SpecialResourceTypesRequiredParametersUtil() {
    }

    public static void checkRequiredParameters(String serviceName, ResourceType resourceType, Map<String, Object> parameters) {
        Set<String> requiredParameters = resourceType.getRequiredParameters();
        for (String parameter : requiredParameters) {
            if (!parameters.containsKey(parameter)) {
                throw new ContentException(Messages.SERVICE_MISSING_REQUIRED_PARAMETER, serviceName, parameter);
            }
        }
    }

}
