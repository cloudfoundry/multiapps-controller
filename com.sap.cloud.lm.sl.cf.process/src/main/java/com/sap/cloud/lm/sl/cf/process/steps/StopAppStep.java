package com.sap.cloud.lm.sl.cf.process.steps;

import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("stopAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class StopAppStep extends SyncActivitiStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        // Get the next cloud application from the context
        CloudApplication app = StepsUtil.getApp(execution.getContext());

        // Get the existing application from the context
        CloudApplication existingApp = StepsUtil.getExistingApp(execution.getContext());

        try {
            if (existingApp != null && !existingApp.getState()
                .equals(AppState.STOPPED)) {
                getStepLogger().info(Messages.STOPPING_APP, app.getName());

                // Get a cloud foundry client
                CloudControllerClient client = execution.getControllerClient();

                // Stop the application
                client.stopApplication(app.getName());

                getStepLogger().debug(Messages.APP_STOPPED, app.getName());
            } else {
                getStepLogger().debug("Application \"{0}\" already stopped", app.getName());
            }

            return StepPhase.DONE;
        } catch (CloudOperationException coe) {
            CloudControllerException e = new CloudControllerException(coe);
            getStepLogger().error(e, Messages.ERROR_STOPPING_APP, app.getName());
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_STOPPING_APP, app.getName());
            throw e;
        }
    }

}
