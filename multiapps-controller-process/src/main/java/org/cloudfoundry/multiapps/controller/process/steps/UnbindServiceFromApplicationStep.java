package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;

import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("unbindServiceFromApplicationStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UnbindServiceFromApplicationStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        String service = context.getVariable(Variables.SERVICE_TO_UNBIND_BIND);
        getStepLogger().info(Messages.UNBINDING_SERVICE_FROM_APP, service, app.getName());

        CloudControllerClient client = context.getControllerClient();
        client.unbindServiceInstance(app.getName(), service);

        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format(Messages.ERROR_WHILE_UNBINDING_SERVICE_FROM_APPLICATION,
                                    context.getVariable(Variables.SERVICE_TO_UNBIND_BIND), context.getVariable(Variables.APP_TO_PROCESS)
                                                                                                  .getName());
    }

}
