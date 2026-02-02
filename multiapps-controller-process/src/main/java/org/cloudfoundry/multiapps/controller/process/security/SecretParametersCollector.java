package org.cloudfoundry.multiapps.controller.process.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.ElementContext;
import org.cloudfoundry.multiapps.mta.model.ExtensionDescriptor;
import org.cloudfoundry.multiapps.mta.model.ExtensionHook;
import org.cloudfoundry.multiapps.mta.model.ExtensionModule;
import org.cloudfoundry.multiapps.mta.model.ExtensionProvidedDependency;
import org.cloudfoundry.multiapps.mta.model.ExtensionRequiredDependency;
import org.cloudfoundry.multiapps.mta.model.ExtensionResource;
import org.cloudfoundry.multiapps.mta.model.Hook;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.ModuleType;
import org.cloudfoundry.multiapps.mta.model.ParametersContainer;
import org.cloudfoundry.multiapps.mta.model.Platform;
import org.cloudfoundry.multiapps.mta.model.PropertiesContainer;
import org.cloudfoundry.multiapps.mta.model.ProvidedDependency;
import org.cloudfoundry.multiapps.mta.model.RequiredDependency;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.cloudfoundry.multiapps.mta.model.ResourceType;
import org.cloudfoundry.multiapps.mta.model.Visitor;
import org.springframework.stereotype.Component;

@Component
public class SecretParametersCollector extends Visitor {

    private final Set<String> secretParameters = new HashSet<>();

    private final MultiValuedMap<String, String> parametersNameValueMap = new ArrayListValuedHashMap<>();

    public Set<String> collectSecrets(DeploymentDescriptor deploymentDescriptor, List<ExtensionDescriptor> extensionDescriptors) {
        deploymentDescriptor.accept(this);

        for (ExtensionDescriptor extensionDescriptor : extensionDescriptors) {
            extensionDescriptor.accept(this);
        }

        Set<String> nestedParameters = new HashSet<>();
        for (Map.Entry<String, Collection<String>> element : parametersNameValueMap.asMap()
                                                                                   .entrySet()) {
            addInNestedParameters(element, nestedParameters);
        }

        Set<String> result = new HashSet<>(secretParameters);
        result.addAll(nestedParameters);
        return result;
    }

    private void addInNestedParameters(Map.Entry<String, Collection<String>> element, Set<String> nestedParameters) {
        String currentParameterName = element.getKey();

        for (String value : element.getValue()) {
            if (value == null) {
                continue;
            }

            shouldBeAddedInNestedParameters(secretParameters, nestedParameters, value, currentParameterName);
        }
    }

    private void shouldBeAddedInNestedParameters(Set<String> secretParameters, Set<String> nestedParameters, String value,
                                                 String currentParameterName) {
        for (String secretParameter : secretParameters) {
            if (!secretParameter.isEmpty() && value.contains(secretParameter)) {
                nestedParameters.add(currentParameterName);
            }
        }
    }

    @Override
    public void visit(ElementContext context, DeploymentDescriptor deploymentDescriptor) {
        collectParameters(deploymentDescriptor);
    }

    @Override
    public void visit(ElementContext context, Module module) {
        collectParametersProperties(module);
    }

    @Override
    public void visit(ElementContext context, ProvidedDependency providedDependency) {
        collectParametersProperties(providedDependency);
    }

    @Override
    public void visit(ElementContext context, RequiredDependency requiredDependency) {
        collectParametersProperties(requiredDependency);
    }

    @Override
    public void visit(ElementContext context, Resource resource) {
        collectParametersProperties(resource);
    }

    @Override
    public void visit(ElementContext context, Hook hook) {
        collectParameters(hook);
    }

    @Override
    public void visit(ElementContext context, ExtensionDescriptor extensionDescriptor) {
        if (extensionDescriptor.getId()
                               .equals(Constants.SECURE_EXTENSION_DESCRIPTOR_ID) && extensionDescriptor.getParameters() != null) {
            secretParameters.addAll(extensionDescriptor.getParameters()
                                                       .keySet());
        }
        collectParameters(extensionDescriptor);
    }

    @Override
    public void visit(ElementContext context, ExtensionModule extensionModule) {
        collectParametersProperties(extensionModule);
    }

    @Override
    public void visit(ElementContext context, ExtensionProvidedDependency extensionProvidedDependency) {
        collectParametersProperties(extensionProvidedDependency);
    }

    @Override
    public void visit(ElementContext context, ExtensionRequiredDependency extensionRequiredDependency) {
        collectParametersProperties(extensionRequiredDependency);
    }

    @Override
    public void visit(ElementContext context, ExtensionResource extensionResource) {
        collectParametersProperties(extensionResource);
    }

    @Override
    public void visit(ElementContext context, Platform platform) {
        collectParameters(platform);
    }

    @Override
    public void visit(ElementContext context, ResourceType resourceType) {
        collectParameters(resourceType);
    }

    @Override
    public void visit(ElementContext context, ModuleType moduleType) {
        collectParametersProperties(moduleType);
    }

    @Override
    public void visit(ElementContext context, ExtensionHook extensionHook) {
        collectParameters(extensionHook);
    }

    private <T extends ParametersContainer & PropertiesContainer> void collectParametersProperties(T parameterPropertiesContainer) {
        collectParameters(parameterPropertiesContainer);
        collectProperties(parameterPropertiesContainer);
    }

    private void collectParameters(ParametersContainer parametersContainer) {
        Map<String, Object> parameters = parametersContainer.getParameters();
        for (Map.Entry<String, Object> parameter : parameters.entrySet()) {
            addValue(parameter.getKey(), parameter.getValue());
        }
    }

    private void collectProperties(PropertiesContainer propertiesContainer) {
        Map<String, Object> properties = propertiesContainer.getProperties();
        for (Map.Entry<String, Object> property : properties.entrySet()) {
            addValue(property.getKey(), property.getValue());
        }
    }

    private void addValue(String name, Object value) {
        if (value == null) {
            parametersNameValueMap.put(name, "");
        } else {
            parametersNameValueMap.put(name, String.valueOf(value));
        }
    }

}
