package com.sap.cloud.lm.sl.cf.core.cf.util;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.SetUtils;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.mta.model.v2.Module;

public class UnresolvedModulesContentValidator implements ModulesContentValidator {

    private Set<String> allMtaModules;
    private Set<String> deployedModules;

    public UnresolvedModulesContentValidator(Set<String> allMtaModules, Set<String> deployedModules) {
        this.allMtaModules = allMtaModules;
        this.deployedModules = deployedModules;
    }

    @Override
    public void validate(List<Module> modules) throws ContentException {
        Set<String> unresolvedModules = getUnresolvedModules(modules);
        if (unresolvedModules.isEmpty()) {
            return;
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

}
