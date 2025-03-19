package org.cloudfoundry.multiapps.controller.process.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Hook;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.ProvidedDependency;
import org.cloudfoundry.multiapps.mta.model.RequiredDependency;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.cloudfoundry.multiapps.mta.resolvers.ReferencePattern;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import jakarta.inject.Named;

@Named("referenceFinder")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ReferenceFinder {

    private String parameterToFind;

    private void setParameterToFind(String parameterToFind) {
        this.parameterToFind = parameterToFind;
    }

    private List<String> getReferencePatterns() {
        return Arrays.asList(ReferencePattern.PLACEHOLDER.getPattern(), ReferencePattern.SHORT.getPattern(),
                             ReferencePattern.FULLY_QUALIFIED.getPattern());
    }

    private String getParameterToFind() {
        return parameterToFind;
    }

    public boolean isParameterReferenced(DeploymentDescriptor descriptor, String parameterToFind) {
        setParameterToFind(parameterToFind);
        return hasReferenceInModules(descriptor) || hasReferenceInResources(descriptor) || hasReferenceInGlobalParameters(descriptor);
    }

    private boolean hasReferenceInModules(DeploymentDescriptor descriptor) {
        return descriptor.getModules()
                         .stream()
                         .anyMatch(this::hasReferenceInModuleParameters);
    }

    private boolean hasReferenceInResources(DeploymentDescriptor descriptor) {
        return descriptor.getResources()
                         .stream()
                         .anyMatch(this::hasReferenceInResourceParameters);
    }

    private boolean hasReferenceInGlobalParameters(DeploymentDescriptor descriptor) {
        return hasReferenceInParameters(descriptor.getParameters());
    }

    private boolean hasReferenceInModuleParameters(Module module) {
        if (hasReferenceInParametersOrProperties(module.getParameters(), module.getProperties())) {
            return true;
        }

        if (hasReferenceInDependency(module.getRequiredDependencies(), module.getProvidedDependencies())) {
            return true;
        }

        return hasReferenceInHooks(getModuleHooks(module));
    }

    private boolean hasReferenceInParametersOrProperties(Map<String, Object> parameters, Map<String, Object> properties) {
        return hasReferenceInParameters(parameters) || hasReferenceInParameters(properties);
    }

    private boolean hasReferenceInParameters(Map<String, Object> parameters) {
        return parameters.entrySet()
                         .stream()
                         .anyMatch(parameterEntry -> hasReferenceInValue(parameterEntry.getValue()));
    }

    private boolean hasReferenceInValue(Object value) {
        if (value instanceof String) {
            List<String> patterns = getReferencePatterns();
            return referencePatternMatches(patterns, value);
        } else if (value instanceof Map<?, ?> map) {
            return map.values()
                      .stream()
                      .anyMatch(this::hasReferenceInValue);
        } else if (value instanceof List<?> list) {
            return list.stream()
                       .anyMatch(this::hasReferenceInValue);
        }
        return false;
    }

    private boolean referencePatternMatches(List<String> patterns, Object value) {
        for (String pattern : patterns) {
            Pattern compiledPattern = Pattern.compile(pattern);
            Matcher matcher = compiledPattern.matcher(MiscUtil.cast(value));
            if (referenceMatchesValue(matcher)) {
                return true;
            }
        }
        return false;
    }

    private boolean referenceMatchesValue(Matcher matcher) {
        if (matcher.find()) {
            String extractedValue = matcher.group(1);
            return extractedValue.equals(getParameterToFind());
        }
        return false;
    }

    private boolean hasReferenceInDependency(List<? extends RequiredDependency> requiredDependencies,
                                             List<? extends ProvidedDependency> providedDependencies) {
        return hasReferenceInRequiredDependencies(requiredDependencies) || hasReferenceInProvidedDependencies(providedDependencies);
    }

    private boolean hasReferenceInRequiredDependencies(List<? extends RequiredDependency> dependencies) {
        return dependencies.stream()
                           .anyMatch(dependency -> hasReferenceInParametersOrProperties(dependency.getParameters(),
                                                                                        dependency.getProperties()));
    }

    private boolean hasReferenceInProvidedDependencies(List<? extends ProvidedDependency> dependencies) {
        return dependencies.stream()
                           .anyMatch(dependency -> hasReferenceInParametersOrProperties(dependency.getParameters(),
                                                                                        dependency.getProperties()));
    }

    private boolean hasReferenceInHooks(List<Hook> hooks) {
        return hooks.stream()
                    .anyMatch(this::hasReferenceInHook);
    }

    private boolean hasReferenceInHook(Hook hook) {
        boolean parametersCheck = hasReferenceInParameters(hook.getParameters());

        boolean dependenciesCheck = hasReferenceInRequiredDependencies(hook.getRequiredDependencies());

        return parametersCheck || dependenciesCheck;
    }

    private boolean hasReferenceInResourceParameters(Resource resource) {
        return hasReferenceInParametersOrProperties(resource.getParameters(), resource.getProperties())
            || hasReferenceInRequiredDependencies(getResourceRequiredDependencies(resource));
    }

    private List<Hook> getModuleHooks(Module module) {
        return SchemaVersionUtils.getEntityData(module, Module::getMajorSchemaVersion, Module::getHooks);
    }

    private List<RequiredDependency> getResourceRequiredDependencies(Resource resource) {
        return SchemaVersionUtils.getEntityData(resource, Resource::getMajorSchemaVersion, Resource::getRequiredDependencies);
    }
}
