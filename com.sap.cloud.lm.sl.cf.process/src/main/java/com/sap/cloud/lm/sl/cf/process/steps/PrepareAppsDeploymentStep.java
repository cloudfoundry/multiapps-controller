package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import javax.inject.Inject;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.util.Configuration;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Component("prepareAppsDeploymentStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PrepareAppsDeploymentStep extends SyncActivitiStep {

    @Inject
    private Configuration configuration;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        getStepLogger().logActivitiTask();

        getStepLogger().info(Messages.PREPARING_APPS_DEPLOYMENT);

        // Get the list of cloud applications from the context:
        List<CloudApplicationExtended> apps = StepsUtil.getAppsToDeploy(execution.getContext());

        // Initialize the iteration over the applications list:
        execution.getContext().setVariable(Constants.VAR_APPS_COUNT, apps.size());
        execution.getContext().setVariable(Constants.VAR_APPS_INDEX, 0);
        execution.getContext().setVariable(Constants.VAR_INDEX_VARIABLE_NAME, Constants.VAR_APPS_INDEX);

        execution.getContext().setVariable(Constants.VAR_CONTROLLER_POLLING_INTERVAL, configuration.getControllerPollingInterval());
        execution.getContext().setVariable(Constants.VAR_UPLOAD_APP_TIMEOUT, configuration.getUploadAppTimeout());
        execution.getContext().setVariable(Constants.VAR_PLATFORM_TYPE, configuration.getPlatformType().toString());

        execution.getContext().setVariable(Constants.REBUILD_APP_ENV, true);
        execution.getContext().setVariable(Constants.SHOULD_UPLOAD_APPLICATION_CONTENT, true);
        execution.getContext().setVariable(Constants.EXECUTE_ONE_OFF_TASKS, true);

        return StepPhase.DONE;
    }

}
