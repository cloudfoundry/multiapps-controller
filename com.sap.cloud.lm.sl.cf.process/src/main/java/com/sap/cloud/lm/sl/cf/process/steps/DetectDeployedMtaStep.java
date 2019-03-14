package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.List;
import java.util.function.Function;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.detect.DeployedComponentsDetector;
import com.sap.cloud.lm.sl.cf.core.model.DeployedComponents;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Component("detectDeployedMtaStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetectDeployedMtaStep extends SyncFlowableStep {

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    protected Function<List<CloudApplication>, DeployedComponents> componentsDetector = deployedApps -> new DeployedComponentsDetector()
        .detectAllDeployedComponents(deployedApps);

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        try {
            getStepLogger().debug(Messages.DETECTING_DEPLOYED_MTA);

            CloudControllerClient client = execution.getControllerClient();

            List<CloudApplication> deployedApps = client.getApplications(false);
            StepsUtil.setDeployedApps(execution.getContext(), deployedApps);
            String mtaId = (String) execution.getContext()
                .getVariable(Constants.PARAM_MTA_ID);

            DeployedMta deployedMta = componentsDetector.apply(deployedApps)
                .findDeployedMta(mtaId);
            if (deployedMta == null) {
                getStepLogger().info(Messages.NO_DEPLOYED_MTA_DETECTED);
            } else {
                getStepLogger().debug(Messages.DEPLOYED_MTA, JsonUtil.toJson(deployedMta, true));
                getStepLogger().info(MessageFormat.format(Messages.DEPLOYED_MTA_DETECTED_WITH_VERSION, deployedMta.getMetadata().getId(), deployedMta.getMetadata().getVersion()));
            }
            StepsUtil.setDeployedMta(execution.getContext(), deployedMta);
            getStepLogger().debug(Messages.DEPLOYED_APPS, secureSerializer.toJson(deployedApps));
            return StepPhase.DONE;
        } catch (CloudOperationException coe) {
            CloudControllerException e = new CloudControllerException(coe);
            getStepLogger().error(e, Messages.ERROR_DETECTING_DEPLOYED_MTA);
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_DETECTING_DEPLOYED_MTA);
            throw e;
        }
    }

}
