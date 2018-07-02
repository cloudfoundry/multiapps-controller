package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.k8s.KubernetesModelRepresenter;
import com.sap.cloud.lm.sl.cf.core.k8s.v3_1.ConfigMapsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.k8s.v3_1.DeploymentsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.k8s.v3_1.JobsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.k8s.v3_1.DockerSecretsCloudModelBuilder;
import com.sap.cloud.lm.sl.mta.model.v3_1.DeploymentDescriptor;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Job;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.extensions.Deployment;

@Component("buildKubernetesCloudModelStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class BuildKubernetesCloudModelStep extends SyncActivitiStep {

    private final Yaml yaml = new Yaml(new KubernetesModelRepresenter());

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws Exception {
        DeploymentDescriptor deploymentDescriptor = (DeploymentDescriptor) StepsUtil.getDeploymentDescriptor(execution.getContext());
        PropertiesAccessor propertiesAccessor = new PropertiesAccessor();
        List<Job> jobs = new JobsCloudModelBuilder(propertiesAccessor).build(deploymentDescriptor);
        showKubernetesResourcesAsYaml(jobs);
        List<Deployment> deployments = new DeploymentsCloudModelBuilder(propertiesAccessor).build(deploymentDescriptor);
        showKubernetesResourcesAsYaml(deployments);
        List<ConfigMap> configMaps = new ConfigMapsCloudModelBuilder().build(deploymentDescriptor);
        showKubernetesResourcesAsYaml(configMaps);
        List<Secret> secrets = new DockerSecretsCloudModelBuilder(propertiesAccessor).build(deploymentDescriptor);
        showKubernetesResourcesAsYaml(secrets);
        return StepPhase.DONE;
    }

    private void showKubernetesResourcesAsYaml(List<? extends HasMetadata> resources) {
        for (HasMetadata resource : resources) {
            ObjectMeta resourceMetadata = resource.getMetadata();
            getStepLogger().info("----------------- " + resourceMetadata.getName() + ".yaml -----------------");
            getStepLogger().info(yaml.dumpAsMap(resource));
        }
    }

}
