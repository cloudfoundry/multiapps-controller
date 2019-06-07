package com.sap.cloud.lm.sl.cf.process.steps;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Component("scaleAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ScaleAppStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        CloudApplication app = StepsUtil.getApp(execution.getContext());

        CloudApplication existingApp = StepsUtil.getExistingApp(execution.getContext());

        getStepLogger().debug(Messages.SCALING_APP, app.getName());

        CloudControllerClient client = execution.getControllerClient();

        String appName = app.getName();
        Integer instances = (app.getInstances() != 0) ? app.getInstances() : null;

        if (instances != null && (existingApp == null || !instances.equals(existingApp.getInstances()))) {
            getStepLogger().info(Messages.SCALING_APP_0_TO_X_INSTANCES, appName, instances);
            client.updateApplicationInstances(appName, instances);
        }

        getStepLogger().debug(Messages.APP_SCALED, app.getName());
        return StepPhase.DONE;
    }

    @Override
    protected void onStepError(DelegateExecution context, Exception e) throws Exception {
        getStepLogger().error(e, Messages.ERROR_SCALING_APP, StepsUtil.getApp(context)
            .getName());
        throw e;
    }

}
