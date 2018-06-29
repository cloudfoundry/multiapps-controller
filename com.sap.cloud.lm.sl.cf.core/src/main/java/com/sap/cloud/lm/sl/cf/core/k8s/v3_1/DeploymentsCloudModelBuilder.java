package com.sap.cloud.lm.sl.cf.core.k8s.v3_1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.util.ListUtil;
import com.sap.cloud.lm.sl.mta.model.ParametersContainer;
import com.sap.cloud.lm.sl.mta.model.v3_1.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v3_1.Module;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentSpec;
import io.fabric8.kubernetes.api.model.extensions.DeploymentSpecBuilder;
import io.fabric8.kubernetes.api.model.extensions.DeploymentStrategy;
import io.fabric8.kubernetes.api.model.extensions.DeploymentStrategyBuilder;
import io.fabric8.kubernetes.api.model.extensions.RollingUpdateDeployment;

public class DeploymentsCloudModelBuilder {

    private static final String CONTAINER_IMAGE_FOR_MODULE_0_IS_NOT_SPECIFIED = "Container image for module \"{0}\" is not specified. Use the \"container-image\" parameter to do so.";

    private static final String LABEL_RELEASE = "release";
    private static final String LABEL_RELEASE_VALUE = "{{ .Release.Name }}";
    private static final String LABEL_APP = "app";
    private static final String LABEL_RUN = "run";
    private static final String LABEL_RUN_SUFFIX = "-pod";
    private static final String TYPE_DEPLOYMENT = "deployment";
    private static final String STRATEGY = "RollingUpdate";
    private static final IntOrString ROLLING_UPDATE_MAX_SURGE = new IntOrString(1);
    private static final IntOrString ROLLING_UPDATE_MAX_UNAVAILABLE = new IntOrString(1);
    private static final Integer REVISION_HISTORY_LIMIT = 10;
    private static final Integer PROGRESS_DEADLINE_IN_SECONDS = 600;
    private static final Integer DEFAULT_CONTAINER_PORT = 8080;
    private static final String DEFAULT_PROTOCOL = "TCP";
    private static final String TERMINATION_MESSAGE_PATH = "/dev/termination-log";
    private static final String TERMINATION_MESSAGE_POLICY = "File";
    private static final String IMAGE_PULL_POLICY = "Always";
    private static final String RESTART_POLICY = "Always";
    private static final Long TERMINATION_GRACE_PERIOD_IN_SECONDS = 30L;
    private static final String DNS_POLICY = "ClusterFirst";
    private static final String SCHEDULER_NAME = "default-scheduler";

    private final PropertiesAccessor propertiesAccessor;

    public DeploymentsCloudModelBuilder(PropertiesAccessor propertiesAccessor) {
        this.propertiesAccessor = propertiesAccessor;
    }

    public List<Deployment> build(DeploymentDescriptor descriptor) {
        List<Deployment> result = new ArrayList<>();
        for (Module module : descriptor.getModules3_1()) {
            ListUtil.addNonNull(result, buildIfDeployment(module));
        }
        return result;
    }

    private Deployment buildIfDeployment(Module module) {
        if (!isDeployment(module)) {
            return null;
        }
        return build(module);
    }

    private boolean isDeployment(Module module) {
        Map<String, Object> moduleParameters = propertiesAccessor.getParameters((ParametersContainer) module);
        String type = (String) moduleParameters.getOrDefault(SupportedParameters.TYPE, TYPE_DEPLOYMENT);
        return TYPE_DEPLOYMENT.equals(type);
    }

    private Deployment build(Module module) {
        Deployment deployment = new Deployment();
        deployment.setMetadata(buildDeploymentMeta(module));
        deployment.setSpec(buildDeploymentSpec(module));
        return deployment;
    }

    private ObjectMeta buildDeploymentMeta(Module module) {
        return new ObjectMetaBuilder().withName(module.getName())
            .addToLabels(LABEL_RUN, getRunLabelValue(module))
            .build();
    }

    private DeploymentSpec buildDeploymentSpec(Module module) {
        return new DeploymentSpecBuilder().withReplicas(getReplicas(module))
            .withSelector(buildSelector(module))
            .withStrategy(buildStrategy(module))
            .withRevisionHistoryLimit(REVISION_HISTORY_LIMIT)
            .withProgressDeadlineSeconds(PROGRESS_DEADLINE_IN_SECONDS)
            .withTemplate(buildPodTemplate(module))
            .build();
    }

    private Integer getReplicas(Module module) {
        Map<String, Object> moduleParameters = propertiesAccessor.getParameters((ParametersContainer) module);
        return (Integer) moduleParameters.getOrDefault(SupportedParameters.INSTANCES, 1);
    }

    private LabelSelector buildSelector(Module module) {
        return new LabelSelectorBuilder().addToMatchLabels(LABEL_RUN, getRunLabelValue(module))
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

    private PodTemplateSpec buildPodTemplate(Module module) {
        return new PodTemplateSpecBuilder().withMetadata(buildPodMeta(module))
            .withSpec(buildPodSpec(module))
            .build();
    }

    private ObjectMeta buildPodMeta(Module module) {
        return new ObjectMetaBuilder().addToLabels(LABEL_RELEASE, LABEL_RELEASE_VALUE)
            .addToLabels(LABEL_APP, getAppLabelValue(module))
            .addToLabels(LABEL_RUN, getRunLabelValue(module))
            .build();
    }

    private String getAppLabelValue(Module module) {
        return module.getName();
    }

    private String getRunLabelValue(Module module) {
        return module.getName() + LABEL_RUN_SUFFIX;
    }

    private PodSpec buildPodSpec(Module module) {
        return new PodSpecBuilder().addAllToContainers(buildContainer(module))
            .withRestartPolicy(RESTART_POLICY)
            .withDnsPolicy(DNS_POLICY)
            .withTerminationGracePeriodSeconds(TERMINATION_GRACE_PERIOD_IN_SECONDS)
            .withSchedulerName(SCHEDULER_NAME)
            .build();
    }

    private List<Container> buildContainer(Module module) {
        // TODO: Allow users to specify more than one container.
        return Arrays.asList(new ContainerBuilder().withName(module.getName())
            .withImage(getImage(module))
            .addAllToPorts(buildPorts(module))
            .withTerminationMessagePath(TERMINATION_MESSAGE_PATH)
            .withTerminationMessagePolicy(TERMINATION_MESSAGE_POLICY)
            .withImagePullPolicy(IMAGE_PULL_POLICY)
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

}
