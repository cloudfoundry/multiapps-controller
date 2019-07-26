package com.sap.cloud.lm.sl.cf.core.cf.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.mta.model.Module;

public class UnresolvedModulesContentValidator implements ModulesContentValidator {

    private Set<String> allMtaModules;
    private Set<String> deployedModules;

    public UnresolvedModulesContentValidator(Set<String> allMtaModules, Set<String> deployedModules) {
        this.allMtaModules = allMtaModules;
        this.deployedModules = deployedModules;
    }

    @Override
    public void validate(List<Module> modules) {
        Set<String> unresolvedModules = getUnresolvedModules(modules);
        if (!unresolvedModules.isEmpty()) {
            throw new ContentException(Messages.UNRESOLVED_MTA_MODULES, unresolvedModules);
        }
    }

    private Set<String> getUnresolvedModules(List<Module> calculatedModules) {
        Set<String> calculatedModuleNames = calculatedModules.stream()
            .map(Module::getName)
            .collect(Collectors.toSet());
        Set<String> resolvedModuleNames = getResolvedModuleNames(calculatedModuleNames);

        return allMtaModules.stream()
            .filter(module -> !resolvedModuleNames.contains(module))
            .collect(Collectors.toSet());
    }

    private Set<String> getResolvedModuleNames(Set<String> moduleNames) {
        Set<String> resolvedModuleNames = new HashSet<>(moduleNames);
        resolvedModuleNames.addAll(deployedModules);
        return resolvedModuleNames;
    }

}
