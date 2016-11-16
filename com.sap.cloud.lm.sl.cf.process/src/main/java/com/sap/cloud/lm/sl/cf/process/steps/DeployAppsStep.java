package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.slp.model.LoopStepMetadata;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("deployAppsStep")
public class DeployAppsStep extends AbstractXS2ProcessStep {

    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(DeployAppsStep.class);

    public static StepMetadata getMetadata() {
        return new LoopStepMetadata("deployAppsTask", "Deploy Apps", "Deploy Apps",
            new StepMetadata[] { CheckAppStep.getMetadata(), CreateAppStep.getMetadata(), StopAppStep.getMetadata(),
                UpdateAppStep.getMetadata(), UploadAppStep.getMetadata(), ScaleAppStep.getMetadata(), StartAppStep.getMetadata() },
            Constants.VAR_APPS_SIZE);
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) {
        logActivitiTask(context, LOGGER);

        info(context, Messages.PREPARING_APPS_DEPLOYMENT, LOGGER);

        // Get the list of cloud applications from the context:
        List<CloudApplicationExtended> apps = StepsUtil.getAppsToDeploy(context);

        // Initialize the iteration over the applications list:
        context.setVariable(Constants.VAR_APPS_SIZE, apps.size());
        context.setVariable(Constants.VAR_APPS_INDEX, 0);

        context.setVariable(Constants.VAR_CONTROLLER_POLLING_INTERVAL, ConfigurationUtil.getControllerPollingInterval());
        context.setVariable(Constants.VAR_UPLOAD_APP_TIMEOUT, ConfigurationUtil.getUploadAppTimeout());

        debug(context, Messages.APPS_DEPLOYMENT_PREPARED, LOGGER);
        return ExecutionStatus.SUCCESS;
    }

}
