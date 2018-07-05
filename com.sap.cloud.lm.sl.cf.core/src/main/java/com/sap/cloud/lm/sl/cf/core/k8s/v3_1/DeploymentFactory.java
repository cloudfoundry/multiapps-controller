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
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.EnvFromSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.extensions.DeploymentSpec;
import io.fabric8.kubernetes.api.model.extensions.DeploymentSpecBuilder;
import io.fabric8.kubernetes.api.model.extensions.DeploymentStrategy;
import io.fabric8.kubernetes.api.model.extensions.DeploymentStrategyBuilder;
import io.fabric8.kubernetes.api.model.extensions.RollingUpdateDeployment;

public class DeploymentFactory implements ResourceFactory {

    private static final String CONTAINER_IMAGE_FOR_MODULE_0_IS_NOT_SPECIFIED = "Container image for module \"{0}\" is not specified. Use the \"container-image\" parameter to do so.";

    private static final String STRATEGY = "RollingUpdate";
    private static final IntOrString ROLLING_UPDATE_MAX_SURGE = new IntOrString(1);
    private static final IntOrString ROLLING_UPDATE_MAX_UNAVAILABLE = new IntOrString(1);
    private static final Integer REVISION_HISTORY_LIMIT = 10;
    private static final Integer PROGRESS_DEADLINE_IN_SECONDS = 600;

    private static final String RESTART_POLICY = "Always";
    private static final Long TERMINATION_GRACE_PERIOD_IN_SECONDS = 30L;
    private static final String DNS_POLICY = "ClusterFirst";
    private static final String SCHEDULER_NAME = "default-scheduler";

    private static final String TERMINATION_MESSAGE_PATH = "/dev/termination-log";
    private static final String TERMINATION_MESSAGE_POLICY = "File";
    private static final String IMAGE_PULL_POLICY = "Always";
    static final Integer DEFAULT_CONTAINER_PORT = 8080;
    static final String DEFAULT_PROTOCOL = "TCP";

    private final DescriptorHandler handler;
    private final PropertiesAccessor propertiesAccessor;

    public DeploymentFactory(DescriptorHandler handler, PropertiesAccessor propertiesAccessor) {
        this.handler = handler;
        this.propertiesAccessor = propertiesAccessor;
    }

    @Override
    public List<String> getSupportedResourceTypes() {
        return Arrays.asList(ResourceTypes.DEPLOYMENT);
    }

    @Override
    public List<HasMetadata> createFrom(DeploymentDescriptor descriptor, Module module, Map<String, String> labels) {
        ConfigMap configMap = buildConfigMap(descriptor, module, labels);
        Deployment deployment = buildDeployment(descriptor, module, configMap, labels);
        return Arrays.asList(configMap, deployment);
    }

    private ConfigMap buildConfigMap(DeploymentDescriptor descriptor, Module module, Map<String, String> labels) {
        return new ConfigMapFactory(handler, propertiesAccessor).createFrom(descriptor, module, labels);
    }

    private Deployment buildDeployment(DeploymentDescriptor descriptor, Module module, ConfigMap configMap, Map<String, String> labels) {
        return new DeploymentBuilder().withMetadata(buildDeploymentMeta(module, labels))
            .withSpec(buildDeploymentSpec(descriptor, module, configMap, labels))
            .build();
    }

    private ObjectMeta buildDeploymentMeta(Module module, Map<String, String> labels) {
        return new ObjectMetaBuilder().withName(module.getName())
            .withLabels(labels)
            .build();
    }

    private DeploymentSpec buildDeploymentSpec(DeploymentDescriptor descriptor, Module module, ConfigMap configMap,
        Map<String, String> labels) {
        return new DeploymentSpecBuilder().withReplicas(getReplicas(module))
            .withSelector(buildSelector(module, labels))
            .withStrategy(buildStrategy(module))
            .withRevisionHistoryLimit(REVISION_HISTORY_LIMIT)
            .withProgressDeadlineSeconds(PROGRESS_DEADLINE_IN_SECONDS)
            .withTemplate(buildPodTemplate(descriptor, module, configMap, labels))
            .build();
    }

    private Integer getReplicas(Module module) {
        Map<String, Object> moduleParameters = propertiesAccessor.getParameters((ParametersContainer) module);
        return (Integer) moduleParameters.getOrDefault(SupportedParameters.INSTANCES, 1);
    }

    private LabelSelector buildSelector(Module module, Map<String, String> labels) {
        return new LabelSelectorBuilder().addToMatchLabels(Labels.APP, labels.get(Labels.APP))
            .build();
    }

    private DeploymentStrategy buildStrategy(Module module) {
        RollingUpdateDeployment rollingUpdateDeployment = new RollingUpdateDeployment();
        rollingUpdateDeployment.setMaxSurge(ROLLING_UPDATE_MAX_SURGE);
        rollingUpdateDeployment.setMaxUnavailable(ROLLING_UPDATE_MAX_UNAVAILABLE);
        return new DeploymentStrategyBuilder().withType(STRATEGY)
            .withRollingUpdate(rollingUpdateDeployment)
            .build();
    }

    private PodTemplateSpec buildPodTemplate(DeploymentDescriptor descriptor, Module module, ConfigMap configMap,
        Map<String, String> labels) {
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
            .withDnsPolicy(DNS_POLICY)
            .withTerminationGracePeriodSeconds(TERMINATION_GRACE_PERIOD_IN_SECONDS)
            .withSchedulerName(SCHEDULER_NAME)
            .withImagePullSecrets(buildImagePullSecrets(descriptor, module))
            .build();
    }

    private List<Container> buildContainers(Module module, ConfigMap configMap) {
        // TODO: Allow users to specify more than one container.
        return Arrays.asList(new ContainerBuilder().withName(module.getName())
            .withImage(getImage(module))
            .addAllToPorts(buildPorts(module))
            .withTerminationMessagePath(TERMINATION_MESSAGE_PATH)
            .withTerminationMessagePolicy(TERMINATION_MESSAGE_POLICY)
            .withImagePullPolicy(IMAGE_PULL_POLICY)
            .withEnvFrom(buildEnvSource(configMap))
            .build());
    }

    private List<ContainerPort> buildPorts(Module module) {
        // TODO: Allow users to specify custom container ports.
        return Arrays.asList(new ContainerPortBuilder().withContainerPort(DEFAULT_CONTAINER_PORT)
            .withProtocol(DEFAULT_PROTOCOL)
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
