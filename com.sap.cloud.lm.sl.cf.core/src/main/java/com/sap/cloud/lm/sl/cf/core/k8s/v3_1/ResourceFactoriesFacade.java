package com.sap.cloud.lm.sl.cf.core.k8s.v3_1;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.k8s.Labels;
import com.sap.cloud.lm.sl.cf.core.k8s.ResourceTypes;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.mta.handlers.v3_1.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.model.ParametersContainer;
import com.sap.cloud.lm.sl.mta.model.v3_1.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v3_1.Module;
import com.sap.cloud.lm.sl.mta.model.v3_1.Resource;

import io.fabric8.kubernetes.api.model.HasMetadata;

public class ResourceFactoriesFacade {

    private final List<ResourceFactory> resourceFactories;
    private final PropertiesAccessor propertiesAccessor;

    public ResourceFactoriesFacade(DescriptorHandler handler, PropertiesAccessor propertiesAccessor) {
        this.resourceFactories = createResourceFactories(handler, propertiesAccessor);
        this.propertiesAccessor = propertiesAccessor;
    }

    private List<ResourceFactory> createResourceFactories(DescriptorHandler handler, PropertiesAccessor propertiesAccessor) {
        List<ResourceFactory> resourceFactories = new ArrayList<>();
        resourceFactories.add(new DeploymentFactory(handler, propertiesAccessor));
        resourceFactories.add(new JobFactory(handler, propertiesAccessor));
        resourceFactories.add(new DockerSecretFactory(propertiesAccessor));
        resourceFactories.add(new IngressFactory(propertiesAccessor));
        resourceFactories.add(new ServiceFactory(propertiesAccessor));
        resourceFactories.add(new SecretFactory(propertiesAccessor));
        resourceFactories.add(new ServiceInstanceFactory(propertiesAccessor));
        return resourceFactories;
    }

    public List<HasMetadata> createFrom(DeploymentDescriptor descriptor) {
        List<HasMetadata> kubernetesResources = new ArrayList<>();
        for (Module module : descriptor.getModules3_1()) {
            kubernetesResources.addAll(createFrom(descriptor, module));
        }
        for (Resource resource : descriptor.getResources3_1()) {
            kubernetesResources.addAll(createFrom(descriptor, resource));
        }
        return kubernetesResources;
    }

    private List<HasMetadata> createFrom(DeploymentDescriptor descriptor, Module module) {
        List<HasMetadata> kubernetesResources = new ArrayList<>();
        String type = getType(module);
        Map<String, String> labels = createLabels(descriptor, module);
        for (ResourceFactory resourceFactory : resourceFactories) {
            if (resourceFactory.getSupportedResourceTypes()
                .contains(type)) {
                kubernetesResources.addAll(resourceFactory.createFrom(descriptor, module, labels));
            }
        }
        return kubernetesResources;
    }

    private String getType(ParametersContainer parametersContainer) {
        Map<String, Object> parameters = propertiesAccessor.getParameters(parametersContainer);
        return (String) parameters.getOrDefault(SupportedParameters.TYPE, ResourceTypes.DEPLOYMENT);
    }

    private Map<String, String> createLabels(DeploymentDescriptor descriptor, Module module) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(Labels.APP, module.getName());
        labels.put(Labels.MTA, descriptor.getId());
        return labels;
    }

    private List<HasMetadata> createFrom(DeploymentDescriptor descriptor, Resource resource) {
        List<HasMetadata> kubernetesResources = new ArrayList<>();
        String type = getType(resource);
        Map<String, String> labels = createLabels(descriptor, resource);
        for (ResourceFactory resourceFactory : resourceFactories) {
            if (resourceFactory.getSupportedResourceTypes()
                .contains(type)) {
                kubernetesResources.addAll(resourceFactory.createFrom(descriptor, resource, labels));
            }
        }
        return kubernetesResources;
    }

    private Map<String, String> createLabels(DeploymentDescriptor descriptor, Resource resource) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(Labels.MTA, descriptor.getId());
        return labels;
    }

}
