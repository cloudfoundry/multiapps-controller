package com.sap.cloud.lm.sl.cf.process.steps;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("stopAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class StopAppStep extends AbstractXS2ProcessStep {

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("stopAppTask").displayName("Stop App").description("Stop App").build();
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {

        getStepLogger().logActivitiTask();

        // Get the next cloud application from the context
        CloudApplication app = StepsUtil.getApp(context);

        // Get the existing application from the context
        CloudApplication existingApp = StepsUtil.getExistingApp(context);

        try {
            if (existingApp != null && !existingApp.getState().equals(AppState.STOPPED)) {
                getStepLogger().info(Messages.STOPPING_APP, app.getName());

                // Get a cloud foundry client
                CloudFoundryOperations client = getCloudFoundryClient(context);

                // Stop the application
                client.stopApplication(app.getName());

                getStepLogger().debug(Messages.APP_STOPPED, app.getName());
            } else {
                getStepLogger().debug("Application \"{0}\" already stopped", app.getName());
            }

            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_STOPPING_APP, app.getName());
            throw e;
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            getStepLogger().error(e, Messages.ERROR_STOPPING_APP, app.getName());
            throw e;
        }
    }

}
