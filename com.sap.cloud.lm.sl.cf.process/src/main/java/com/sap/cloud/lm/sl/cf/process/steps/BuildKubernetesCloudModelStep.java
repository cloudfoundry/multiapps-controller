package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.k8s.KubernetesModelRepresenter;
import com.sap.cloud.lm.sl.cf.core.k8s.v3_1.ResourceFactoriesFacade;
import com.sap.cloud.lm.sl.mta.handlers.v3_1.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.model.v3_1.DeploymentDescriptor;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;

@Component("buildKubernetesCloudModelStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class BuildKubernetesCloudModelStep extends SyncActivitiStep {

    private final Yaml yaml = new Yaml(new KubernetesModelRepresenter());

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws Exception {
        DeploymentDescriptor deploymentDescriptor = (DeploymentDescriptor) StepsUtil.getDeploymentDescriptor(execution.getContext());
        ResourceFactoriesFacade resourceFactoriesFacade = new ResourceFactoriesFacade(new DescriptorHandler(), new PropertiesAccessor());
        List<? extends HasMetadata> kubernetesResources = resourceFactoriesFacade.createFrom(deploymentDescriptor);
        showKubernetesResourcesAsYaml(execution, kubernetesResources);
        return StepPhase.DONE;
    }

    private void showKubernetesResourcesAsYaml(ExecutionWrapper execution, List<? extends HasMetadata> resources) {
        for (HasMetadata resource : resources) {
            ObjectMeta resourceMetadata = resource.getMetadata();
            Logger logger = createLogger(execution, resourceMetadata.getName() + ".yaml");
            logger.info(yaml.dumpAsMap(resource));
        }
    }

    private Logger createLogger(ExecutionWrapper execution, String fileName) {
        return getProcessLoggerProvider().getLoggerProvider(fileName)
            .getLogger(StepsUtil.getCorrelationId(execution.getContext()), "com.sap.cloud.lm.sl.xs2", fileName);
    }

}
