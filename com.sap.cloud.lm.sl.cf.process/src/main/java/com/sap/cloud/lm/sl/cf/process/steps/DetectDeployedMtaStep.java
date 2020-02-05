package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.core.cf.detect.DeployedMtaDetector;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Named("detectDeployedMtaStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetectDeployedMtaStep extends SyncFlowableStep {

    @Inject
    private DeployedMtaDetector deployedMtaDetector;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        getStepLogger().debug(Messages.DETECTING_DEPLOYED_MTA);

        CloudControllerClient client = execution.getControllerClient();

        String mtaId = (String) execution.getContext()
                                         .getVariable(Constants.PARAM_MTA_ID);
        Optional<DeployedMta> optionalDeployedMta = deployedMtaDetector.detectDeployedMta(mtaId, client);
        if (optionalDeployedMta.isPresent()) {
            DeployedMta deployedMta = optionalDeployedMta.get();
            StepsUtil.setDeployedMta(execution.getContext(), deployedMta);
            getStepLogger().debug(Messages.DEPLOYED_MTA, JsonUtil.toJson(deployedMta, true));
            getStepLogger().info(MessageFormat.format(Messages.DEPLOYED_MTA_DETECTED_WITH_VERSION, deployedMta.getMetadata()
                                                                                                              .getId(),
                                                      deployedMta.getMetadata()
                                                                 .getVersion()));
        } else {
            getStepLogger().info(Messages.NO_DEPLOYED_MTA_DETECTED);
            StepsUtil.setDeployedMta(execution.getContext(), null);
        }
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(DelegateExecution context) {
        return Messages.ERROR_DETECTING_DEPLOYED_MTA;
    }
}
