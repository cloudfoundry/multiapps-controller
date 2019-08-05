package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.detect.DeployedComponentsDetector;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Component("detectDeployedMtaStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetectDeployedMtaStep extends SyncFlowableStep {

    @Autowired
    private DeployedComponentsDetector deployedComponentsDetector;

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        getStepLogger().debug(Messages.DETECTING_DEPLOYED_MTA);

        CloudControllerClient client = execution.getControllerClient();

        List<CloudApplication> deployedApps = client.getApplications();
        StepsUtil.setDeployedApps(execution.getContext(), deployedApps);
        String mtaId = (String) execution.getContext()
                                         .getVariable(Constants.PARAM_MTA_ID);

            Optional<DeployedMta> optionalDeployedMta = deployedComponentsDetector.getDeployedMta(mtaId, client);
            if (optionalDeployedMta.isPresent()) {
                DeployedMta deployedMta = optionalDeployedMta.get();
                StepsUtil.setDeployedMta(execution.getContext(), deployedMta);
                getStepLogger().debug(Messages.DEPLOYED_MTA, JsonUtil.toJson(deployedMta, true));
                getStepLogger().info(MessageFormat.format(Messages.DEPLOYED_MTA_DETECTED_WITH_VERSION, deployedMta.getMetadata().getId(), deployedMta.getMetadata().getVersion()));
            } else {
                getStepLogger().info(Messages.NO_DEPLOYED_MTA_DETECTED);
                StepsUtil.setDeployedMta(execution.getContext(), null);
            }
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

    @Override
    protected String getStepErrorMessage(DelegateExecution context) {
        return Messages.ERROR_DETECTING_DEPLOYED_MTA;
    }
}
