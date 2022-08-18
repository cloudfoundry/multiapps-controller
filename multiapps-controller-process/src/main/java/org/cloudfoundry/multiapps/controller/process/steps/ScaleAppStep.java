package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
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
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        CloudApplication existingApp = context.getVariable(Variables.EXISTING_APP);
        CloudControllerClient client = context.getControllerClient();

        getStepLogger().debug(Messages.SCALING_APP, app.getName());
        int desiredInstances = app.getInstances();
        int currentInstances = 1; //default instances when creating an app
        if (existingApp != null) {
            currentInstances = client.getApplicationInstances(existingApp)
                                     .getInstances()
                                     .size();
        }

        if (desiredInstances != currentInstances) {
            getStepLogger().info(Messages.SCALING_APP_0_TO_X_INSTANCES, app.getName(), desiredInstances);
            client.updateApplicationInstances(app.getName(), desiredInstances);
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
