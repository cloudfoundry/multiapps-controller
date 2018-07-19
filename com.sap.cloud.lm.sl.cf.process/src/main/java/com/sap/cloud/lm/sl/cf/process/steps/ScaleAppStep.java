package com.sap.cloud.lm.sl.cf.process.steps;

import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("scaleAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ScaleAppStep extends SyncActivitiStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws SLException {
        CloudApplication app = StepsUtil.getApp(execution.getContext());

        CloudApplication existingApp = StepsUtil.getExistingApp(execution.getContext());

        try {
            getStepLogger().info(Messages.SCALING_APP, app.getName());

            CloudControllerClient client = execution.getControllerClient();

            String appName = app.getName();
            Integer instances = (app.getInstances() != 0) ? app.getInstances() : null;

            if (instances != null && (existingApp == null || !instances.equals(existingApp.getInstances()))) {
                getStepLogger().debug("Updating instances of application \"{0}\"", appName);
                client.updateApplicationInstances(appName, instances);
            }

            getStepLogger().debug(Messages.APP_SCALED, app.getName());
            return StepPhase.DONE;
        } catch (CloudOperationException coe) {
            CloudControllerException e = new CloudControllerException(coe);
            getStepLogger().error(e, Messages.ERROR_SCALING_APP, app.getName());
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_SCALING_APP, app.getName());
            throw e;
        }
    }

}
