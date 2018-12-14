package com.sap.cloud.lm.sl.cf.core.cf.util;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.SetUtils;

import com.sap.cloud.lm.sl.cf.core.helpers.ModuleToDeployHelper;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.mta.model.v2.Module;

public class ModulesCloudModelBuilderContentCalculator implements CloudModelBuilderContentCalculator<Module> {

    private Set<String> mtaModulesInArchive;
    private Set<String> deployedModules;
    private Set<String> allMtaModules;
    private PropertiesAccessor propertiesAccessor;
    private List<String> modulesSpecifiedForDeployment;
    private UserMessageLogger userMessageLogger;
    private ModuleToDeployHelper moduleToDeployHelper;

    public ModulesCloudModelBuilderContentCalculator(Set<String> mtaModulesInArchive, Set<String> deployedModules,
        Set<String> allMtaModules, List<String> modulesSpecifiedForDeployment, PropertiesAccessor propertiesAccessor,
        UserMessageLogger userMessageLogger, ModuleToDeployHelper moduleToDeployHelper) {
        this.mtaModulesInArchive = mtaModulesInArchive;
        this.deployedModules = deployedModules;
        this.modulesSpecifiedForDeployment = modulesSpecifiedForDeployment;
        this.propertiesAccessor = propertiesAccessor;
        this.userMessageLogger = userMessageLogger;
        this.allMtaModules = allMtaModules;
        this.moduleToDeployHelper = moduleToDeployHelper;
    }

    @Override
    public List<Module> calculateContentForBuilding(List<? extends Module> modulesForDeployment) {
        initializeModulesDependecyTypes(modulesForDeployment);
        List<Module> calculatedModules = modulesForDeployment.stream()
            .filter(module -> shouldDeployModule(module, mtaModulesInArchive, deployedModules))
            .filter(this::isModuleSpecifiedForDeployment)
            .collect(Collectors.toList());
        Set<String> unresolvedModules = getUnresolvedModules(calculatedModules);
        if (unresolvedModules.isEmpty()) {
            return calculatedModules;
        }
        throw new ContentException(Messages.UNRESOLVED_MTA_MODULES, unresolvedModules);
    }

    private Set<String> getUnresolvedModules(List<Module> calculatedModules) {
        Set<String> calculatedModuleNames = calculatedModules.stream()
            .map(Module::getName)
            .collect(Collectors.toSet());
        return SetUtils.difference(allMtaModules, SetUtils.union(calculatedModuleNames, deployedModules))
            .toSet();
    }

    private void initializeModulesDependecyTypes(List<? extends Module> modulesForDeployment) {
        for (Module module : modulesForDeployment) {
            String dependencyType = getDependencyType(propertiesAccessor, module);
            Map<String, Object> moduleProperties = propertiesAccessor.getParameters(module);
            moduleProperties.put(SupportedParameters.DEPENDENCY_TYPE, dependencyType);
            propertiesAccessor.setParameters(module, moduleProperties);
        }
    }

    protected String getDependencyType(PropertiesAccessor propertiesAccessor, Module module) {
        return (String) propertiesAccessor.getParameters(module)
            .getOrDefault(SupportedParameters.DEPENDENCY_TYPE, com.sap.cloud.lm.sl.cf.core.Constants.DEPENDENCY_TYPE_SOFT);
    }

    private boolean isModuleSpecifiedForDeployment(Module module) {
        return modulesSpecifiedForDeployment.isEmpty() || modulesSpecifiedForDeployment.contains(module.getName());
    }

    private boolean shouldDeployModule(Module module, Set<String> mtaModulesInArchive, Set<String> deployedModules) {
        if (moduleToDeployHelper.shouldDeployAlways(module) || isDockerModule(module)) {
            return true;
        }
        if (!isModulePresentInArchive(module, mtaModulesInArchive) || module.getType() == null) {
            if (isModuleDeployed(module, deployedModules)) {
                printMTAModuleNotFoundWarning(module.getName());
            }
            return false;
        }

        return true;
    }

    private void printMTAModuleNotFoundWarning(String name) {
        if (userMessageLogger == null) {
            return;
        }
        userMessageLogger.warn(Messages.NOT_DESCRIBED_MODULE, name);
    }

    private boolean isDockerModule(Module module) {
        Map<String, Object> moduleParameters = propertiesAccessor.getParameters(module);

        return moduleParameters.containsKey(SupportedParameters.DOCKER);
    }

    private boolean isModulePresentInArchive(Module module, Set<String> modulesInArchive) {
        return modulesInArchive.contains(module.getName());
    }

    private boolean isModuleDeployed(Module module, Set<String> deployedModules) {
        return deployedModules.contains(module.getName());
    }

}
