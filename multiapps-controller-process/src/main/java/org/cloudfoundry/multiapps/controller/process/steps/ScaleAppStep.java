package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;

@Named("scaleAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ScaleAppStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        CloudApplication app = context.getVariable(Variables.APP_TO_PROCESS);
        CloudApplication existingApp = context.getVariable(Variables.EXISTING_APP);
        CloudControllerClient client = context.getControllerClient();

        getStepLogger().debug(Messages.SCALING_APP, app.getName());
        String appName = app.getName();
        int instances = app.getInstances();

        if (existingApp == null || instances != existingApp.getInstances()) {
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
