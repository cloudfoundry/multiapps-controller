package com.sap.cloud.lm.sl.cf.process.steps;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("checkAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CheckAppStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        // Get the next cloud application from the context:
        CloudApplication app = StepsUtil.getApp(execution.getContext());

        try {
            getStepLogger().debug(Messages.CHECKING_APP, app.getName());

            CloudControllerClient client = execution.getControllerClient();

            // Check if an application with this name already exists, and store it in the context:
            CloudApplication existingApp = client.getApplication(app.getName(), false);
            StepsUtil.setExistingApp(execution.getContext(), existingApp);

            if (existingApp == null) {
                getStepLogger().debug(Messages.APP_DOES_NOT_EXIST, app.getName());
            } else {
                getStepLogger().debug(Messages.APP_EXISTS, app.getName());
            }

            return StepPhase.DONE;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_CHECKING_APP, app.getName());
            throw e;
        } catch (CloudOperationException coe) {
            CloudControllerException e = new CloudControllerException(coe);
            getStepLogger().error(e, Messages.ERROR_CHECKING_APP, app.getName());
            throw e;
        }
    }

}
