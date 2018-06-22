package com.sap.cloud.lm.sl.cf.core.util;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.sap.cloud.lm.sl.cf.core.cf.v1_0.ResourceType;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.parser.ParametersParser;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Module;
import com.sap.cloud.lm.sl.mta.model.v1_0.Platform;
import com.sap.cloud.lm.sl.mta.model.v1_0.ProvidedDependency;
import com.sap.cloud.lm.sl.mta.model.v1_0.Resource;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;

public class CloudModelBuilderUtil {

    public static boolean isPublic(ProvidedDependency dependency) {
        if (dependency instanceof com.sap.cloud.lm.sl.mta.model.v2_0.ProvidedDependency) {
            return ((com.sap.cloud.lm.sl.mta.model.v2_0.ProvidedDependency) dependency).isPublic();
        }
        return true;
    }

    public static Target findTarget(DescriptorHandler handler, List<Target> targets, String targetName, Target defaultTarget)
        throws ContentException {
        Target target = handler.findTarget(targets, targetName, defaultTarget);
        if (target == null) {
            throw new ContentException(Messages.UNKNOWN_TARGET, targetName);
        }
        return target;
    }

    public static Platform findPlatform(DescriptorHandler handler, List<Platform> platforms, Target target) throws ContentException {
        Platform platform = handler.findPlatform(platforms, target.getType());
        if (platform == null) {
            throw new ContentException(Messages.UNKNOWN_PLATFORM, target.getType(), target.getName());
        }
        return platform;
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

    public static String buildImplicitDeployTargetName(String org, String space) {
        return String.format("%s %s", org, space);
    }

    public static boolean isService(Resource resource, PropertiesAccessor propertiesAccessor) {
        Set<ResourceType> resourceTypes = ResourceType.getServiceTypes();
        ResourceType resourceType = getResourceType(resource, propertiesAccessor);
        return resourceTypes.contains(resourceType);
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
        for (Module mtaModule : deploymentDescriptor.getModules1_0()) {
            deployedModuleNames.add(mtaModule.getName());
        }
        return deployedModuleNames;
    }
}
