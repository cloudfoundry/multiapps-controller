package org.cloudfoundry.multiapps.controller.process.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Hook;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.RequiredDependency;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named("supportedParametersChecker")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SupportedParametersChecker {

    private List<String> unknownParameters;

    @Inject
    private ReferenceFinder finder;

    public List<String> getUnknownParameters(DeploymentDescriptor descriptor) {
        List<String> unsupportedAndNotReferencedParameters = new ArrayList<>();
        for (String parameter : findUnsupportedParameters(descriptor)) {
            if (!this.finder.isParameterReferenced(descriptor, parameter)) {
                unsupportedAndNotReferencedParameters.add(parameter);
            }
        }
        return unsupportedAndNotReferencedParameters;
    }

    private List<String> findUnsupportedParameters(DeploymentDescriptor descriptor) {
        this.unknownParameters = new ArrayList<>();
        descriptor.getModules()
                  .forEach(this::checkModuleParameters);
        descriptor.getResources()
                  .forEach(this::checkResourceParameters);
        checkParameters(getGlobalParameters(), descriptor.getParameters());
        return unknownParameters;
    }

    private void checkModuleParameters(Module module) {
        checkParameters(getModuleParameters(), module.getParameters());
        getModuleHooks(module).forEach(this::checkHookParameters);
        module.getRequiredDependencies()
              .forEach(dependency -> checkParameters(getDependencyParameters(), dependency.getParameters()));
        module.getProvidedDependencies()
              .forEach(dependency -> checkParameters(getDependencyParameters(), dependency.getParameters()));

    }

    protected Set<String> getDependencyParameters() {
        Set<String> dependencyParameters = new HashSet<>();
        dependencyParameters.addAll(SupportedParameters.DEPENDENCY_PARAMETERS);
        dependencyParameters.addAll(getModuleParameters());
        dependencyParameters.addAll(getResourceParameters());
        return dependencyParameters;
    }

    protected Set<String> getModuleParameters() {
        return SupportedParameters.MODULE_PARAMETERS;
    }

    protected Set<String> getModuleHookParameters() {
        return SupportedParameters.MODULE_HOOK_PARAMETERS;
    }

    protected Set<String> getResourceParameters() {
        return SupportedParameters.RESOURCE_PARAMETERS;
    }

    protected Set<String> getGlobalParameters() {
        return SupportedParameters.GLOBAL_PARAMETERS;
    }

    private void checkResourceParameters(Resource resource) {
        checkParameters(getResourceParameters(), resource.getParameters());
        checkRequiredDependencyParameters(getResourceRequiredDependencies(resource));
    }

    private void checkHookParameters(Hook hook) {
        checkParameters(getModuleHookParameters(), hook.getParameters());
        checkRequiredDependencyParameters(hook.getRequiredDependencies());
    }

    private void checkRequiredDependencyParameters(List<RequiredDependency> requiredDependencies) {
        requiredDependencies.forEach(requiredDependency -> checkParameters(getDependencyParameters(), requiredDependency.getParameters()));
    }

    private void checkParameters(Set<String> supportedParameters, Map<String, Object> parameters) {
        for (String parameterName : parameters.keySet()) {
            if (!supportedParameters.contains(parameterName) && !getGlobalParameters().contains(parameterName)) {
                unknownParameters.add(parameterName);
            }
        }
    }

    private List<Hook> getModuleHooks(Module module) {
        return (List<Hook>) SchemaVersionUtils.getEntityData(module, Module::getMajorSchemaVersion, Module::getHooks);
    }

    private List<RequiredDependency> getResourceRequiredDependencies(Resource resource) {
        return (List<RequiredDependency>) SchemaVersionUtils.getEntityData(resource, Resource::getMajorSchemaVersion,
                                                                           Resource::getRequiredDependencies);
    }

}
