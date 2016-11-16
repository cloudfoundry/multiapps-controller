package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.List;
import java.util.function.Function;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.cf.detect.DeployedComponentsDetector;
import com.sap.cloud.lm.sl.cf.core.model.DeployedComponents;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("detectDeployedMtaStep")
public class DetectDeployedMtaStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(DetectDeployedMtaStep.class);

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    public static StepMetadata getMetadata() {
        return new StepMetadata("detectDeployedMtaTask", "Detect Deployed MTA", "Detect Deployed MTA");
    }

    protected Function<List<CloudApplication>, DeployedComponents> componentsDetector = (
        deployedApps) -> new DeployedComponentsDetector().detectAllDeployedComponents(deployedApps);

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        logActivitiTask(context, LOGGER);

        try {
            info(context, Messages.DETECTING_DEPLOYED_MTA, LOGGER);

            CloudFoundryOperations client = getCloudFoundryClient(context, LOGGER);

            List<CloudApplication> deployedApps = client.getApplications();
            StepsUtil.setDeployedApps(context, deployedApps);
            String mtaId = (String) context.getVariable(Constants.PARAM_MTA_ID);

            DeployedMta deployedMta = componentsDetector.apply(deployedApps).findDeployedMta(mtaId);
            if (deployedMta == null) {
                info(context, Messages.NO_DEPLOYED_MTA_DETECTED, LOGGER);
            } else {
                debug(context, format(Messages.DEPLOYED_MTA, JsonUtil.toJson(deployedMta, true)), LOGGER);
                info(context, Messages.DEPLOYED_MTA_DETECTED, LOGGER);
            }
            StepsUtil.setDeployedMta(context, deployedMta);
            debug(context, format(Messages.DEPLOYED_APPS, secureSerializer.toJson(deployedApps)), LOGGER);
            return ExecutionStatus.SUCCESS;
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            error(context, Messages.ERROR_DETECTING_DEPLOYED_MTA, e, LOGGER);
            throw e;
        } catch (SLException e) {
            error(context, Messages.ERROR_DETECTING_DEPLOYED_MTA, e, LOGGER);
            throw e;
        }
    }

}
