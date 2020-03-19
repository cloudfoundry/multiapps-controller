package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;

import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

@Named("scaleAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ScaleAppStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        CloudApplication app = context.getVariable(Variables.APP_TO_PROCESS);

        CloudApplication existingApp = context.getVariable(Variables.EXISTING_APP);

        getStepLogger().debug(Messages.SCALING_APP, app.getName());

        CloudControllerClient client = context.getControllerClient();

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
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format(Messages.ERROR_SCALING_APP, context.getVariable(Variables.APP_TO_PROCESS)
                                                                       .getName());
    }

}
