package org.cloudfoundry.multiapps.controller.process.util;

import java.util.HashSet;
import java.util.Set;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.mta.resolvers.ParameterChecker;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("supportedParameterChecker")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SupportedParameterChecker extends ParameterChecker {

    private static final Set<String> REQUIRED_AND_PROVIDED_DEPENDENCY_PARAMETERS = initializeDependencyParameters();
    
    @Override
    protected Set<String> getModuleParametersToMatch() {
        return SupportedParameters.MODULE_PARAMETERS;
    }

    @Override
    protected Set<String> getModuleHookParametersToMatch() {
        return SupportedParameters.MODULE_HOOK_PARAMETERS;
    }

    @Override
    protected Set<String> getResourceParametersToMatch() {
        return SupportedParameters.RESOURCE_PARAMETERS;
    }

    @Override
    protected Set<String> getGlobalParametersToMatch() {
        return SupportedParameters.GLOBAL_PARAMETERS;
    }

    @Override
    protected Set<String> getDependencyParametersToMatch() {
        return REQUIRED_AND_PROVIDED_DEPENDENCY_PARAMETERS;
    }

    private static Set<String> initializeDependencyParameters() {
        Set<String> dependencyParameters = new HashSet<>();
        dependencyParameters.addAll(SupportedParameters.DEPENDENCY_PARAMETERS);
        dependencyParameters.addAll(SupportedParameters.MODULE_PARAMETERS);
        dependencyParameters.addAll(SupportedParameters.RESOURCE_PARAMETERS);
        return dependencyParameters;
    }

}
