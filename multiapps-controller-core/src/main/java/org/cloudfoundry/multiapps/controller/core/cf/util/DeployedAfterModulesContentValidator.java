package org.cloudfoundry.multiapps.controller.core.cf.util;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.helpers.ModuleToDeployHelper;
import org.cloudfoundry.multiapps.controller.core.util.UserMessageLogger;
import org.cloudfoundry.multiapps.mta.model.Module;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;

public class DeployedAfterModulesContentValidator implements ModulesContentValidator {

    private static final String DEFAULT_MESSAGE_DELIMITER = ", ";

    private final CloudControllerClient client;
    private final UserMessageLogger userMessageLogger;
    private final ModuleToDeployHelper moduleToDeployHelper;
    private final Map<String, Module> allModulesInArchive;

    public DeployedAfterModulesContentValidator(CloudControllerClient client, UserMessageLogger userMessageLogger,
                                                ModuleToDeployHelper moduleToDeployHelper, List<Module> allMtaModules) {
        this.client = client;
        this.userMessageLogger = userMessageLogger;
        this.moduleToDeployHelper = moduleToDeployHelper;
        this.allModulesInArchive = allMtaModules.stream()
                                                .collect(Collectors.toMap(Module::getName, Function.identity()));
    }

    @Override
    public void validate(List<Module> modules) {
        Set<String> moduleNamesForDeployment = getModuleNames(modules);

        List<String> modulesWithDependenciesNotDeployed = modules.stream()
                                                                 .filter(module -> module.getMajorSchemaVersion() >= 3)
                                                                 .filter(module -> !areModuleDependenciesResolvable(module,
                                                                                                                    moduleNamesForDeployment))
                                                                 .map(Module::getName)
                                                                 .collect(Collectors.toList());

        if (!modulesWithDependenciesNotDeployed.isEmpty()) {
            throw new ContentException(Messages.UNRESOLVED_MODULE_DEPENDENCIES,
                                       String.join(DEFAULT_MESSAGE_DELIMITER, modulesWithDependenciesNotDeployed));
        }
    }

    private boolean areModuleDependenciesResolvable(Module module, Set<String> moduleNamesForDeployment) {
        List<String> moduleDependencies = module.getDeployedAfter();
        if (moduleDependencies == null) {
            return true;
        }
        return moduleDependencies.stream()
                                 .allMatch(dependency -> isModuleDependencyResolvable(moduleNamesForDeployment, module.getName(), dependency));
    }

    private boolean isModuleDependencyResolvable(Set<String> moduleNamesForDeployment, String moduleWithDependency, String dependencyModule) {
        if (moduleNamesForDeployment.contains(dependencyModule)) {
            return true;
        }
        Module dependencyNotForDeployment = allModulesInArchive.get(dependencyModule);
        if (dependencyNotForDeployment != null && !moduleToDeployHelper.isApplication(dependencyNotForDeployment)) {
            userMessageLogger.warn(MessageFormat.format(Messages.MODULE_0_DEPENDS_ON_MODULE_1_WHICH_CANNOT_BE_RESOLVED,
                                                        moduleWithDependency, dependencyModule));
            return true;
        }
        return client.getApplication(dependencyModule, false) != null;
    }

    private Set<String> getModuleNames(List<Module> modules) {
        return modules.stream()
                      .map(Module::getName)
                      .collect(Collectors.toSet());
    }

}
