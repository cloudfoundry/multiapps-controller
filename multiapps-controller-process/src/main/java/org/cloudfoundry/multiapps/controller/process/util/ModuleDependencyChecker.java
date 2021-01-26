package org.cloudfoundry.multiapps.controller.process.util;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.helpers.ModuleToDeployHelper;
import org.cloudfoundry.multiapps.controller.core.util.UserMessageLogger;
import org.cloudfoundry.multiapps.mta.model.Module;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;

public class ModuleDependencyChecker {

    private final Map<String, Module> allModulesInDescriptorWithNames;
    private final Set<String> modulesForDeployment;
    private final Set<String> modulesNotForDeployment;
    private final Set<String> modulesAlreadyDeployed;
    private final CloudControllerClient client;
    private final ModuleToDeployHelper moduleToDeployHelper;
    private final UserMessageLogger userMessageLogger;

    public ModuleDependencyChecker(CloudControllerClient client, UserMessageLogger userMessageLogger,
                                   ModuleToDeployHelper moduleToDeployHelper, List<Module> allModulesInDescriptor,
                                   List<Module> allModulesToDeploy, List<Module> completedModules) {
        this.client = client;
        this.userMessageLogger = userMessageLogger;
        this.moduleToDeployHelper = moduleToDeployHelper;
        allModulesInDescriptorWithNames = computeAllModulesMap(allModulesInDescriptor);
        modulesForDeployment = computeModules(allModulesToDeploy);
        modulesAlreadyDeployed = computeModules(completedModules);
        modulesNotForDeployment = computeModules(allModulesInDescriptor, module -> !modulesForDeployment.contains(module));
    }

    private Map<String, Module> computeAllModulesMap(List<Module> allModules) {
        return allModules.stream()
                         .collect(Collectors.toMap(Module::getName, Function.identity()));
    }

    private Set<String> computeModules(List<Module> modules) {
        return computeModules(modules, m -> true);
    }

    private Set<String> computeModules(List<Module> modules, Predicate<String> filterFunction) {
        return modules.stream()
                      .map(Module::getName)
                      .filter(filterFunction)
                      .collect(Collectors.toSet());
    }

    public boolean areAllDependenciesSatisfied(Module module) {
        if (module.getMajorSchemaVersion() < 3) {
            return true;
        }
        return module.getDeployedAfter()
                     .isEmpty()
            || areDependenciesProcessed(module) || areAllDependenciesAlreadyPresent(module);
    }

    private boolean areAllDependenciesAlreadyPresent(Module module) {
        boolean allModulesFoundInSpace = module.getDeployedAfter()
                                               .stream()
                                               .allMatch(dependency -> isDependencyPresent(module.getName(), dependency));
        List<String> modulesNotYetDeployed = module.getDeployedAfter().stream()
                                                                      .filter(modulesForDeployment::contains)
                                                                      .filter(dependency -> !modulesAlreadyDeployed.contains(dependency))
                                                                      .collect(Collectors.toList());
        return allModulesFoundInSpace && modulesNotYetDeployed.isEmpty();
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

    private boolean areDependenciesProcessed(Module module) {
        return module.getDeployedAfter()
                     .stream()
                     .allMatch(this::isProcessed);
    }

    private boolean isProcessed(String moduleName) {
        return modulesAlreadyDeployed.contains(moduleName) || modulesNotForDeployment.contains(moduleName);
    }

    // if the module has a deployed-after, which is not specified for deployment, we check if it is a CF app
    private boolean isDependencyPresent(String moduleName, String dependencyName) {
        if (!modulesNotForDeployment.contains(dependencyName)) {
            return true;
        }
        Module dependency = allModulesInDescriptorWithNames.get(dependencyName);
        if (!moduleToDeployHelper.isApplication(dependency)) {
            userMessageLogger.warn(MessageFormat.format(Messages.MODULE_0_DEPENDS_ON_MODULE_1_WHICH_CANNOT_BE_RESOLVED,
                                                        moduleName, dependencyName));
            return true;
        }
        return client.getApplication(dependencyName, false) != null;
    }
}
