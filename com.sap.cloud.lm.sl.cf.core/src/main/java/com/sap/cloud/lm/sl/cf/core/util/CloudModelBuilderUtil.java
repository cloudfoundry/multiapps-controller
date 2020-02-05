package com.sap.cloud.lm.sl.cf.core.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.sap.cloud.lm.sl.cf.core.cf.v2.ResourceType;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.parser.ParametersParser;
import com.sap.cloud.lm.sl.mta.model.Resource;

public class CloudModelBuilderUtil {

    private static final ApplicationColor COLOR_OF_APPLICATIONS_WITHOUT_SUFFIX = ApplicationColor.BLUE;

    private CloudModelBuilderUtil() {
    }

    public static Set<String> getDeployedModuleNames(List<DeployedMtaApplication> deployedApplications) {
        return deployedApplications.stream()
                                   .map(DeployedMtaApplication::getModuleName)
                                   .collect(Collectors.toCollection(TreeSet::new));
    }

    public static boolean isService(Resource resource) {
        Set<ResourceType> resourceTypes = ResourceType.getServiceTypes();
        ResourceType resourceType = getResourceType(resource);
        return resourceTypes.contains(resourceType);
    }

    public static boolean isServiceKey(Resource resource) {
        if (resource.getType() == null) {
            return false;
        }
        return ResourceType.EXISTING_SERVICE_KEY.equals(getResourceType(resource));
    }

    public static <R> R parseParameters(List<Map<String, Object>> parametersList, ParametersParser<R> parser) {
        return parser.parse(parametersList);
    }

    public static ResourceType getResourceType(Map<String, Object> properties) {
        String type = (String) properties.getOrDefault(SupportedParameters.TYPE, ResourceType.MANAGED_SERVICE.toString());
        return ResourceType.get(type);
    }

    public static ApplicationColor getApplicationColor(DeployedMtaApplication deployedApplication) {
        return Arrays.stream(ApplicationColor.values())
                     .filter(color -> deployedApplication.getName()
                                                         .endsWith(color.asSuffix()))
                     .findFirst()
                     .orElse(COLOR_OF_APPLICATIONS_WITHOUT_SUFFIX);
    }

    private static ResourceType getResourceType(Resource resource) {
        Map<String, Object> resourceParameters = resource.getParameters();
        String type = (String) resourceParameters.get(SupportedParameters.TYPE);
        return ResourceType.get(type);
    }

}
