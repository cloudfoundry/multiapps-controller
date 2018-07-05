package com.sap.cloud.lm.sl.cf.core.k8s.v3_1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.k8s.Labels;
import com.sap.cloud.lm.sl.cf.core.k8s.ResourceTypes;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.mta.handlers.v3_1.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.model.ParametersContainer;
import com.sap.cloud.lm.sl.mta.model.v3_1.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v3_1.Module;
import com.sap.cloud.lm.sl.mta.model.v3_1.RequiredDependency;
import com.sap.cloud.lm.sl.mta.model.v3_1.Resource;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapEnvSource;
import io.fabric8.kubernetes.api.model.ConfigMapEnvSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.EnvFromSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Job;
import io.fabric8.kubernetes.api.model.JobBuilder;
import io.fabric8.kubernetes.api.model.JobSpec;
import io.fabric8.kubernetes.api.model.JobSpecBuilder;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;

public class JobFactory implements ResourceFactory {

    private static final String CONTAINER_IMAGE_FOR_MODULE_0_IS_NOT_SPECIFIED = "Container image for module \"{0}\" is not specified. Use the \"container-image\" parameter to do so.";

    private static final String RESTART_POLICY = "OnFailure";
    private static final String IMAGE_PULL_POLICY = "Always";

    private final DescriptorHandler handler;
    private final PropertiesAccessor propertiesAccessor;

    public JobFactory(DescriptorHandler handler, PropertiesAccessor propertiesAccessor) {
        this.handler = handler;
        this.propertiesAccessor = propertiesAccessor;
    }

    @Override
    public List<String> getSupportedResourceTypes() {
        return Arrays.asList(ResourceTypes.JOB);
    }

    @Override
    public List<HasMetadata> createFrom(DeploymentDescriptor descriptor, Module module, Map<String, String> labels) {
        ConfigMap configMap = buildConfigMap(descriptor, module, labels);
        Job job = buildJob(descriptor, module, configMap, labels);
        return Arrays.asList(configMap, job);
    }

    private ConfigMap buildConfigMap(DeploymentDescriptor descriptor, Module module, Map<String, String> labels) {
        return new ConfigMapFactory(handler, propertiesAccessor).createFrom(descriptor, module, labels);
    }

    private Job buildJob(DeploymentDescriptor descriptor, Module module, ConfigMap configMap, Map<String, String> labels) {
        return new JobBuilder().withMetadata(buildMeta(module, labels))
            .withSpec(buildSpec(descriptor, module, configMap, labels))
            .build();
    }

    private ObjectMeta buildMeta(Module module, Map<String, String> labels) {
        return new ObjectMetaBuilder().withName(module.getName())
            .withLabels(labels)
            .build();
    }

    private JobSpec buildSpec(DeploymentDescriptor descriptor, Module module, ConfigMap configMap, Map<String, String> labels) {
        return new JobSpecBuilder().withTemplate(buildPodTemplate(descriptor, module, configMap, labels))
            .build();
    }

    // FIXME: Reduce the code duplication between this class and DeploymentsCloudModelBuilder.
    private PodTemplateSpec buildPodTemplate(DeploymentDescriptor descriptor, Module module, ConfigMap configMap, Map<String, String> labels) {
        return new PodTemplateSpecBuilder().withMetadata(buildPodMeta(module, labels))
            .withSpec(buildPodSpec(descriptor, module, configMap))
            .build();
    }

    private ObjectMeta buildPodMeta(Module module, Map<String, String> labels) {
        return new ObjectMetaBuilder().addToLabels(Labels.RELEASE, Labels.RELEASE_VALUE)
            .withLabels(labels)
            .build();
    }

    private PodSpec buildPodSpec(DeploymentDescriptor descriptor, Module module, ConfigMap configMap) {
        return new PodSpecBuilder().addAllToContainers(buildContainers(module, configMap))
            .withRestartPolicy(RESTART_POLICY)
            .withImagePullSecrets(buildImagePullSecrets(descriptor, module))
            .build();
    }

    private List<Container> buildContainers(Module module, ConfigMap configMap) {
        // TODO: Allow users to specify more than one container.
        return Arrays.asList(new ContainerBuilder().withName(module.getName())
            .withImage(getImage(module))
            .withImagePullPolicy(IMAGE_PULL_POLICY)
            .withEnvFrom(buildEnvSource(configMap))
            .build());
    }

    private String getImage(Module module) {
        Map<String, Object> moduleParameters = propertiesAccessor.getParameters((ParametersContainer) module);
        String image = (String) moduleParameters.get(com.sap.cloud.lm.sl.cf.core.k8s.SupportedParameters.CONTAINER_IMAGE);
        if (image == null) {
            throw new ContentException(CONTAINER_IMAGE_FOR_MODULE_0_IS_NOT_SPECIFIED, module.getName());
        }
        return image;
    }

    private EnvFromSource buildEnvSource(ConfigMap configMap) {
        return new EnvFromSourceBuilder().withConfigMapRef(buildConfigMapEnvSource(configMap))
            .build();
    }

    private ConfigMapEnvSource buildConfigMapEnvSource(ConfigMap configMap) {
        String configMapName = configMap.getMetadata()
            .getName();
        return new ConfigMapEnvSourceBuilder().withName(configMapName)
            .build();
    }
    

    private List<LocalObjectReference> buildImagePullSecrets(DeploymentDescriptor descriptor, Module module) {
        List<LocalObjectReference> imagePullSecrets = new ArrayList<>();
        for (RequiredDependency requiredDependency : module.getRequiredDependencies3_1()) {
            Resource resource = (Resource) handler.findResource(descriptor, requiredDependency.getName());
            if (resource == null) {
                continue;
            }
            Map<String, Object> resourceParameters = propertiesAccessor.getParameters((ParametersContainer) resource);
            String resourceType = (String) resourceParameters.get(SupportedParameters.TYPE);
            if (ResourceTypes.DOCKER_SECRET.equals(resourceType)) {
                imagePullSecrets.add(new LocalObjectReference(resource.getName()));
            }
        }
        return imagePullSecrets;
    }

}
