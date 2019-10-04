package com.sap.cloud.lm.sl.cf.process.util;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.cloud.lm.sl.mta.model.Module;

public class ModuleDependencyChecker {

    private final Set<String> modulesForDeployment;
    private final Set<String> modulesNotForDeployment;
    private final Set<String> modulesAlreadyDeployed;
    private final CloudControllerClient client;

    public ModuleDependencyChecker(CloudControllerClient client, List<Module> allModulesInDescriptor, List<Module> allModulesToDeploy,
                                   List<Module> completedModules) {
        this.client = client;

        modulesForDeployment = computeModulesForDeployment(allModulesToDeploy);
        modulesAlreadyDeployed = computeAlreadyDeployedModules(completedModules);
        modulesNotForDeployment = computeModuleNotForDeployment(allModulesInDescriptor);
    }

    private Set<String> computeModulesForDeployment(List<Module> allModulesToDeploy) {
        return allModulesToDeploy.stream()
                                 .map(Module::getName)
                                 .collect(Collectors.toSet());
    }

    private Set<String> computeAlreadyDeployedModules(List<Module> completedModules) {
        return completedModules.stream()
                               .map(Module::getName)
                               .collect(Collectors.toSet());
    }

    private Set<String> computeModuleNotForDeployment(List<Module> allModules) {
        return allModules.stream()
                         .map(Module::getName)
                         .filter(module -> !modulesForDeployment.contains(module))
                         .collect(Collectors.toSet());
    }

    public boolean areAllDependenciesSatisfied(Module module) {
        if (module.getMajorSchemaVersion() < 3) {
            return true;
        }

        return module.getDeployedAfter()
                     .isEmpty()
            || modulesAlreadyDeployed.containsAll(module.getDeployedAfter()) || areAllDependenciesAlreadyPresent(module.getDeployedAfter());
    }

    private boolean areAllDependenciesAlreadyPresent(List<String> deployedAfter) {
        List<CloudApplication> modulesNotFoundInSpace = deployedAfter.stream()
                                                                     .filter(modulesNotForDeployment::contains)
                                                                     .map(deployAfterDependency -> client.getApplication(deployAfterDependency,
                                                                                                                         false))
                                                                     .filter(Objects::isNull)
                                                                     .collect(Collectors.toList());

        List<String> modulesNotYetDeployed = deployedAfter.stream()
                                                          .filter(modulesForDeployment::contains)
                                                          .filter(module -> !modulesAlreadyDeployed.contains(module))
                                                          .collect(Collectors.toList());

        return modulesNotFoundInSpace.isEmpty() && modulesNotYetDeployed.isEmpty();
    }

    public Set<String> getModulesForDeployment() {
        return modulesForDeployment;
    }

    public Set<String> getModulesNotForDeployment() {
        return modulesNotForDeployment;
    }

    public Set<String> getAlreadyDeployedModules() {
        return modulesAlreadyDeployed;
    }
}
