package com.sap.cloud.lm.sl.cf.core.util;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Module;
import com.sap.cloud.lm.sl.mta.model.v1_0.ProvidedDependency;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatformType;

public class CloudModelBuilderUtil {

    public static boolean isPublic(ProvidedDependency dependency) {
        if (dependency instanceof com.sap.cloud.lm.sl.mta.model.v2_0.ProvidedDependency) {
            return ((com.sap.cloud.lm.sl.mta.model.v2_0.ProvidedDependency) dependency).isPublic();
        }
        return true;
    }

    public static TargetPlatform findPlatform(DescriptorHandler handler, List<TargetPlatform> platforms, String platformName,
        TargetPlatform defaultPlatform) throws ContentException {
        TargetPlatform platform = handler.findPlatform(platforms, platformName, defaultPlatform);
        if (platform == null) {
            throw new ContentException(Messages.UNKNOWN_PLATFORM, platformName);
        }
        return platform;
    }

    public static TargetPlatformType findPlatformType(DescriptorHandler handler, List<TargetPlatformType> platformTypes,
        TargetPlatform platform) throws ContentException {
        TargetPlatformType platformType = handler.findPlatformType(platformTypes, platform.getType());
        if (platformType == null) {
            throw new ContentException(Messages.UNKNOWN_PLATFORM_TYPE, platform.getType(), platform.getName());
        }
        return platformType;
    }

    public static Module findModule(DescriptorHandler handler, DeploymentDescriptor deploymentDescriptor, String moduleName)
        throws ContentException {
        Module module = handler.findModule(deploymentDescriptor, moduleName);
        if (module == null) {
            throw new ContentException(Messages.UNKNOWN_MODULE, moduleName);
        }
        return module;
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

    public static String buildImplicitTargetPlatformName(String org, String space) {
        return String.format("%s %s", org, space);
    }

}
