package com.sap.cloud.lm.sl.cf.core.util;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.sap.cloud.lm.sl.cf.core.cf.v2.ResourceType;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.parser.ParametersParser;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.Module;
import com.sap.cloud.lm.sl.mta.model.v2.ProvidedDependency;
import com.sap.cloud.lm.sl.mta.model.v2.Resource;

public class CloudModelBuilderUtil {

    public static boolean isPublic(ProvidedDependency dependency) {
        if (dependency instanceof ProvidedDependency) {
            return dependency.isPublic();
        }
        return true;
    }

    public static Set<String> getDeployedModuleNames(List<DeployedMtaModule> deployedModules) {
        Set<String> deployedModuleNames = new TreeSet<>();
        for (DeployedMtaModule deployedModule : deployedModules) {
            deployedModuleNames.add(deployedModule.getModuleName());
        }
        return deployedModuleNames;
    }

    public static Set<String> getDeployedAppNames(List<DeployedMtaModule> deployedModules) {
        Set<String> deployedAppNames = new TreeSet<>();
        for (DeployedMtaModule deployedModule : deployedModules) {
            deployedAppNames.add(deployedModule.getAppName());
        }
        return deployedAppNames;
    }

    public static boolean isService(Resource resource, PropertiesAccessor propertiesAccessor) {
        Set<ResourceType> resourceTypes = ResourceType.getServiceTypes();
        ResourceType resourceType = getResourceType(resource, propertiesAccessor);
        return resourceTypes.contains(resourceType);
    }

    public static boolean isActive(Resource resource) {
        com.sap.cloud.lm.sl.mta.model.v3.Resource resourceV3 = (com.sap.cloud.lm.sl.mta.model.v3.Resource) resource;
        return resourceV3.isActive();
    }

    public static boolean isServiceKey(Resource resource, PropertiesAccessor propertiesAccessor) {
        if (resource.getType() == null) {
            return false;
        }
        return ResourceType.EXISTING_SERVICE_KEY.equals(getResourceType(resource, propertiesAccessor));
    }

    public static <R> R parseParameters(List<Map<String, Object>> parametersList, ParametersParser<R> parser) {
        return parser.parse(parametersList);
    }

    public static ResourceType getResourceType(Map<String, Object> properties) {
        String type = (String) properties.getOrDefault(SupportedParameters.TYPE, ResourceType.MANAGED_SERVICE.toString());
        return ResourceType.get(type);
    }

    private static ResourceType getResourceType(Resource resource, PropertiesAccessor propertiesAccessor) {
        Map<String, Object> resourceParameters = propertiesAccessor.getParameters(resource);
        String type = (String) resourceParameters.get(SupportedParameters.TYPE);
        return ResourceType.get(type);
    }

    public static Set<String> getModuleNames(DeploymentDescriptor deploymentDescriptor) {
        Set<String> deployedModuleNames = new TreeSet<>();
        for (Module mtaModule : deploymentDescriptor.getModules2()) {
            deployedModuleNames.add(mtaModule.getName());
        }
        return deployedModuleNames;
    }
}
