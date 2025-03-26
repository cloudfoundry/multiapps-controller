package org.cloudfoundry.multiapps.controller.process.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Hook;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.RequiredDependency;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("supportedParametersChecker")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SupportedParametersChecker {

    private List<String> unsupportedParameters;

    @Inject
    private ReferenceFinder finder;

    public List<String> getUnknownParameters(DeploymentDescriptor descriptor) {
        List<String> unsupportedAndNotReferencedParameters = new ArrayList<>();
        findUnsupportedParameters(descriptor);
        for (String unsupportedParameter : getUnsupportedParameters()) {
            if (!this.finder.isParameterReferenced(descriptor, unsupportedParameter)) {
                unsupportedAndNotReferencedParameters.add(unsupportedParameter);
            }
        }
        return unsupportedAndNotReferencedParameters;
    }

    private List<String> getUnsupportedParameters() {
        return unsupportedParameters != null ? unsupportedParameters : Collections.emptyList();
    }

    private void addUnsupportedParameter(String parameterName) {
        if (unsupportedParameters == null) {
            this.unsupportedParameters = new ArrayList<>();
        }
        this.unsupportedParameters.add(parameterName);
    }

    private void findUnsupportedParameters(DeploymentDescriptor descriptor) {
        descriptor.getModules()
                  .forEach(this::checkModuleParameters);
        descriptor.getResources()
                  .forEach(this::checkResourceParameters);
        checkParameters(getGlobalParameters(), descriptor.getParameters());
    }

    private void checkModuleParameters(Module module) {
        checkParameters(getModuleParameters(), module.getParameters());
        getModuleHooks(module).forEach(this::checkHookParameters);
        Set<String> dependencyParameters = getDependencyParameters();
        module.getRequiredDependencies()
              .forEach(dependency -> checkParameters(dependencyParameters, dependency.getParameters()));
        module.getProvidedDependencies()
              .forEach(dependency -> checkParameters(dependencyParameters, dependency.getParameters()));

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
            if (!supportedParameters.contains(parameterName)) {
                addUnsupportedParameter(parameterName);
            }
        }
    }

    private List<Hook> getModuleHooks(Module module) {
        return SchemaVersionUtils.getEntityData(module, Module::getMajorSchemaVersion, Module::getHooks);
    }

    private List<RequiredDependency> getResourceRequiredDependencies(Resource resource) {
        return SchemaVersionUtils.getEntityData(resource, Resource::getMajorSchemaVersion, Resource::getRequiredDependencies);
    }

}
