package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.k8s.v3_1.ConfigMapsCloudModelBuilder;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.util.YamlUtil;

import io.fabric8.kubernetes.api.model.ConfigMap;

@Component("buildKubernetesCloudModelStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class BuildKubernetesCloudModelStep extends SyncActivitiStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws Exception {
        DeploymentDescriptor deploymentDescriptor = StepsUtil.getDeploymentDescriptor(execution.getContext());
        ConfigMapsCloudModelBuilder configMapsBuilder = new ConfigMapsCloudModelBuilder();
        List<ConfigMap> configMaps = configMapsBuilder
            .build((com.sap.cloud.lm.sl.mta.model.v3_1.DeploymentDescriptor) deploymentDescriptor);
        for (ConfigMap configMap : configMaps) {
            getStepLogger().info("----------------- " + configMap.getMetadata()
                .getName() + ".yaml -----------------");
            getStepLogger().info(YamlUtil.convertToYaml(configMap));
        }
        return StepPhase.DONE;
    }

}
