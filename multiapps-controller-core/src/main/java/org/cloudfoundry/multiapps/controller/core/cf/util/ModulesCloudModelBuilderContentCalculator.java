package org.cloudfoundry.multiapps.controller.core.cf.util;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.helpers.ModuleToDeployHelper;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.util.UserMessageLogger;
import org.cloudfoundry.multiapps.mta.model.Module;

public class ModulesCloudModelBuilderContentCalculator implements CloudModelBuilderContentCalculator<Module> {

    private final Set<String> mtaModulesInArchive;
    private final Set<String> deployedModules;
    private final List<String> modulesSpecifiedForDeployment;
    private final UserMessageLogger userMessageLogger;
    private final ModuleToDeployHelper moduleToDeployHelper;
    private final List<ModulesContentValidator> modulesContentValidators;

    public ModulesCloudModelBuilderContentCalculator(Set<String> mtaModulesInArchive, Set<String> deployedModules,
                                                     List<String> modulesSpecifiedForDeployment, UserMessageLogger userMessageLogger,
                                                     ModuleToDeployHelper moduleToDeployHelper,
                                                     List<ModulesContentValidator> modulesContentValidators) {
        this.mtaModulesInArchive = mtaModulesInArchive;
        this.deployedModules = deployedModules;
        this.modulesSpecifiedForDeployment = modulesSpecifiedForDeployment;
        this.userMessageLogger = userMessageLogger;
        this.moduleToDeployHelper = moduleToDeployHelper;
        this.modulesContentValidators = modulesContentValidators;
    }

    @Override
    public List<Module> calculateContentForBuilding(List<? extends Module> modulesForDeployment) {
        initializeModulesDependencyTypes(modulesForDeployment);
        List<Module> calculatedModules = modulesForDeployment.stream()
                                                             .filter(module -> shouldDeployModule(module, mtaModulesInArchive,
                                                                                                  deployedModules))
                                                             .filter(this::isModuleSpecifiedForDeployment)
                                                             .collect(Collectors.toList());
        validateCalculatedModules(calculatedModules);
        return calculatedModules;
    }

    private void validateCalculatedModules(List<Module> calculatedModules) {
        for (ModulesContentValidator validator : modulesContentValidators) {
            validator.validate(calculatedModules);
        }
    }

    private void initializeModulesDependencyTypes(List<? extends Module> modulesForDeployment) {
        for (Module module : modulesForDeployment) {
            String dependencyType = getDependencyType(module);
            Map<String, Object> parameters = new TreeMap<>(module.getParameters());
            parameters.put(SupportedParameters.DEPENDENCY_TYPE, dependencyType);
            module.setParameters(parameters);
        }
    }

    protected String getDependencyType(Module module) {
        return (String) module.getParameters()
                              .getOrDefault(SupportedParameters.DEPENDENCY_TYPE,
                                            org.cloudfoundry.multiapps.controller.core.Constants.DEPENDENCY_TYPE_SOFT);
    }

    private boolean isModuleSpecifiedForDeployment(Module module) {
        return modulesSpecifiedForDeployment == null || modulesSpecifiedForDeployment.contains(module.getName());
    }

    private boolean shouldDeployModule(Module module, Set<String> mtaModulesInArchive, Set<String> deployedModules) {
        if (moduleToDeployHelper.shouldDeployAlways(module) || isDockerModule(module)) {
            return true;
        }
        if (!mtaModulesInArchive.contains(module.getName()) || module.getType() == null) {
            if (deployedModules.contains(module.getName())) {
                printMTAModuleNotFoundWarning(module.getName());
            }
            return false;
        }
        return true;
    }

    private void printMTAModuleNotFoundWarning(String name) {
        if (userMessageLogger != null) {
            userMessageLogger.warn(Messages.NOT_DESCRIBED_MODULE, name);
        }
    }

    private boolean isDockerModule(Module module) {
        return module.getParameters()
                     .containsKey(SupportedParameters.DOCKER);
    }
}
