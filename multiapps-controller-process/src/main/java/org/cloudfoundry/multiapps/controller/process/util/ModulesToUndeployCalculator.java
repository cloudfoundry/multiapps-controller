package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;
import java.util.Set;

import org.cloudfoundry.multiapps.controller.core.helpers.ModuleToDeployHelper;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.mta.model.Module;

public class ModulesToUndeployCalculator {

    private final DeployedMta deployedMta;
    private final Set<String> selectedMtaModuleNames;
    private final List<Module> deploymentDescriptorModules;
    private final List<String> deploymentDescriptorModuleNames;
    private final ModuleToDeployHelper moduleToDeployHelper;

    public ModulesToUndeployCalculator(DeployedMta deployedMta, Set<String> selectedMtaModuleNames,
                                       List<Module> deploymentDescriptorModules, ModuleToDeployHelper moduleToDeployHelper) {
        this.deployedMta = deployedMta;
        this.selectedMtaModuleNames = selectedMtaModuleNames;
        this.deploymentDescriptorModules = deploymentDescriptorModules;
        this.deploymentDescriptorModuleNames = deploymentDescriptorModules.stream()
                                                                          .map(Module::getName)
                                                                          .toList();
        this.moduleToDeployHelper = moduleToDeployHelper;
    }

    public List<DeployedMtaApplication> computeModulesToUndeploy(List<String> appsToDeploy) {
        return deployedMta.getApplications()
                          .stream()
                          .filter(this::shouldBeCheckedForUndeployment)
                          .filter(deployedApplication -> shouldUndeployModule(deployedApplication, appsToDeploy))
                          .toList();
    }

    private boolean shouldBeCheckedForUndeployment(DeployedMtaApplication deployedApplication) {
        boolean isSelectedMtaModule = selectedMtaModuleNames.contains(deployedApplication.getModuleName());
        boolean isNotInDescriptor = !deploymentDescriptorModuleNames.contains(deployedApplication.getModuleName());
        boolean isSkippedModule = shouldUndeploySkippedModule(deployedApplication, deploymentDescriptorModules);
        return isSelectedMtaModule || isNotInDescriptor || isSkippedModule;
    }

    private boolean shouldUndeploySkippedModule(DeployedMtaApplication deployedApplication, List<Module> deploymentDescriptorModules) {
        return deploymentDescriptorModules.stream()
                                          .filter(module -> module.getName()
                                                                  .equals(deployedApplication.getModuleName()))
                                          .anyMatch(moduleToDeployHelper::shouldSkipDeploy);
    }

    private boolean shouldUndeployModule(DeployedMtaApplication deployedMtaApplication, List<String> appsToDeploy) {
        // The deployed module may be in the list of MTA modules, but the actual application that was created from it may have a
        // different name:
        return !appsToDeploy.contains(deployedMtaApplication.getName());
    }

    public List<DeployedMtaApplication> computeModulesWithoutChange(List<DeployedMtaApplication> modulesToUndeploy) {
        return deployedMta.getApplications()
                          .stream()
                          .filter(existingModule -> shouldNotUndeployModule(modulesToUndeploy, existingModule))
                          .filter(this::shouldNotDeployModule)
                          .toList();
    }

    private boolean shouldNotUndeployModule(List<DeployedMtaApplication> modulesToUndeploy, DeployedMtaApplication existingModule) {
        String existingModuleName = existingModule.getModuleName();
        return modulesToUndeploy.stream()
                                .map(DeployedMtaApplication::getModuleName)
                                .noneMatch(existingModuleName::equals);
    }

    private boolean shouldNotDeployModule(DeployedMtaApplication existingModule) {
        String existingModuleName = existingModule.getModuleName();
        return !selectedMtaModuleNames.contains(existingModuleName);
    }
}
