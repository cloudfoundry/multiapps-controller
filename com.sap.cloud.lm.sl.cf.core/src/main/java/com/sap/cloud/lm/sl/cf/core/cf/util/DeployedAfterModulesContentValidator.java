package com.sap.cloud.lm.sl.cf.core.cf.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.mta.model.v2.Module;

public class DeployedAfterModulesContentValidator implements ModulesContentValidator {

    private static final String DEFAULT_MESSAGE_DELIMITER = ", ";

    private CloudControllerClient client;

    public DeployedAfterModulesContentValidator(CloudControllerClient client) {
        this.client = client;
    }

    @Override
    public void validate(List<Module> modules) throws ContentException {
        List<String> modulesInModelNames = getModuleNames(modules);

        Map<String, List<String>> modulesWithDependenciesNotInModel = modules.stream()
            .filter(module -> module instanceof com.sap.cloud.lm.sl.mta.model.v3.Module)
            .map(module -> (com.sap.cloud.lm.sl.mta.model.v3.Module) module)
            .collect(Collectors.toMap(Module::getName, module -> getDependenciesNotInModel(module, modulesInModelNames)));

        List<String> modulesWithDependeciesNotDeployed = modulesWithDependenciesNotInModel.entrySet()
            .stream()
            .filter(moduleWithDependencies -> !areModulesAlreadyDeployed(client, moduleWithDependencies.getValue()))
            .map(moduleWithDependencies -> moduleWithDependencies.getKey())
            .collect(Collectors.toList());

        if (modulesWithDependeciesNotDeployed.isEmpty()) {
            return;
        }
        throw new ContentException(Messages.UNRESOLVED_MODULE_DEPENDENCIES,
            String.join(DEFAULT_MESSAGE_DELIMITER, modulesWithDependeciesNotDeployed));
    }

    private List<String> getDependenciesNotInModel(com.sap.cloud.lm.sl.mta.model.v3.Module module, List<String> modulesInModelNames) {
        Collection<String> moduleDependencies = CollectionUtils.emptyIfNull(module.getDeployedAfter());
        return moduleDependencies.stream()
            .filter(dependency -> !modulesInModelNames.contains(dependency))
            .collect(Collectors.toList());
    }

    private boolean areModulesAlreadyDeployed(CloudControllerClient client, List<String> deployedAfter) {
        List<CloudApplication> dependencyModulesApplications = deployedAfter.stream()
            .map(dependencyModule -> client.getApplication(dependencyModule, false))
            .filter(Objects::isNull)
            .collect(Collectors.toList());
        return dependencyModulesApplications.isEmpty();
    }

    private List<String> getModuleNames(List<Module> modules) {
        return modules.stream()
            .map(Module::getName)
            .collect(Collectors.toList());
    }

}
