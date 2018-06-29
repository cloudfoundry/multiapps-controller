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
import com.sap.cloud.lm.sl.common.util.ListUtil;
import com.sap.cloud.lm.sl.mta.model.ParametersContainer;
import com.sap.cloud.lm.sl.mta.model.v3_1.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v3_1.Module;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.Job;
import io.fabric8.kubernetes.api.model.JobBuilder;
import io.fabric8.kubernetes.api.model.JobSpec;
import io.fabric8.kubernetes.api.model.JobSpecBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;

public class JobsCloudModelBuilder {

    private static final String CONTAINER_IMAGE_FOR_MODULE_0_IS_NOT_SPECIFIED = "Container image for module \"{0}\" is not specified. Use the \"container-image\" parameter to do so.";

    private static final String RESTART_POLICY = "OnFailure";

    private final PropertiesAccessor propertiesAccessor;

    public JobsCloudModelBuilder(PropertiesAccessor propertiesAccessor) {
        this.propertiesAccessor = propertiesAccessor;
    }

    public List<Job> build(DeploymentDescriptor descriptor) {
        List<Job> result = new ArrayList<>();
        for (Module module : descriptor.getModules3_1()) {
            ListUtil.addNonNull(result, buildIfJob(module));
        }
        return result;
    }

    private Job buildIfJob(Module module) {
        if (!isJob(module)) {
            return null;
        }
        return build(module);
    }

    private boolean isJob(Module module) {
        Map<String, Object> moduleParameters = propertiesAccessor.getParameters((ParametersContainer) module);
        String type = (String) moduleParameters.get(SupportedParameters.TYPE);
        return ResourceTypes.JOB.equals(type);
    }

    private Job build(Module module) {
        return new JobBuilder().withMetadata(buildMeta(module))
            .withSpec(buildSpec(module))
            .build();
    }

    private ObjectMeta buildMeta(Module module) {
        return new ObjectMetaBuilder().withName(module.getName())
            .addToLabels(Labels.APP, module.getName())
            .build();
    }

    private JobSpec buildSpec(Module module) {
        return new JobSpecBuilder().withTemplate(buildPodTemplate(module))
            .build();
    }

    // FIXME: Reduce the code duplication between this class and DeploymentsCloudModelBuilder.
    private PodTemplateSpec buildPodTemplate(Module module) {
        return new PodTemplateSpecBuilder().withMetadata(buildPodMeta(module))
            .withSpec(buildPodSpec(module))
            .build();
    }

    private ObjectMeta buildPodMeta(Module module) {
        return new ObjectMetaBuilder().addToLabels(Labels.RELEASE, Labels.RELEASE_VALUE)
            .addToLabels(Labels.APP, getAppLabelValue(module))
            .addToLabels(Labels.RUN, getRunLabelValue(module))
            .build();
    }

    private String getAppLabelValue(Module module) {
        return module.getName();
    }

    private String getRunLabelValue(Module module) {
        return module.getName() + Labels.RUN_SUFFIX;
    }

    private PodSpec buildPodSpec(Module module) {
        return new PodSpecBuilder().addAllToContainers(buildContainer(module))
            .withRestartPolicy(RESTART_POLICY)
            .build();
    }

    private List<Container> buildContainer(Module module) {
        // TODO: Allow users to specify more than one container.
        return Arrays.asList(new ContainerBuilder().withName(module.getName())
            .withImage(getImage(module))
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
