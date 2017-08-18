package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;
import java.util.function.Function;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
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
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetectDeployedMtaStep extends AbstractXS2ProcessStep {

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("detectDeployedMtaTask").displayName("Detect Deployed MTA").description(
            "Detect Deployed MTA").build();
    }

    protected Function<List<CloudApplication>, DeployedComponents> componentsDetector = (
        deployedApps) -> new DeployedComponentsDetector().detectAllDeployedComponents(deployedApps);

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        getStepLogger().logActivitiTask();

        try {
            getStepLogger().info(Messages.DETECTING_DEPLOYED_MTA);

            CloudFoundryOperations client = getCloudFoundryClient(context);

            List<CloudApplication> deployedApps = client.getApplications();
            StepsUtil.setDeployedApps(context, deployedApps);
            String mtaId = (String) context.getVariable(Constants.PARAM_MTA_ID);

            DeployedMta deployedMta = componentsDetector.apply(deployedApps).findDeployedMta(mtaId);
            if (deployedMta == null) {
                getStepLogger().info(Messages.NO_DEPLOYED_MTA_DETECTED);
            } else {
                getStepLogger().debug(Messages.DEPLOYED_MTA, JsonUtil.toJson(deployedMta, true));
                getStepLogger().info(Messages.DEPLOYED_MTA_DETECTED);
            }
            StepsUtil.setDeployedMta(context, deployedMta);
            getStepLogger().debug(Messages.DEPLOYED_APPS, secureSerializer.toJson(deployedApps));
            return ExecutionStatus.SUCCESS;
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            getStepLogger().error(e, Messages.ERROR_DETECTING_DEPLOYED_MTA);
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_DETECTING_DEPLOYED_MTA);
            throw e;
        }
    }

}
