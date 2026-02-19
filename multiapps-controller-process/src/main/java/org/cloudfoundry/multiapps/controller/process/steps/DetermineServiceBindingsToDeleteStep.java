package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;
import java.util.UUID;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceBinding;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.security.serialization.DynamicSecureSerialization;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.security.util.SecureLoggingUtil;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("determineServiceBindingsToDeleteStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetermineServiceBindingsToDeleteStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) throws Exception {
        CloudApplicationExtended appToDelete = context.getVariable(Variables.APP_TO_PROCESS);
        CloudControllerClient controllerClient = context.getControllerClient();
        UUID applicationGuid = controllerClient.getApplicationGuid(appToDelete.getName());
        List<CloudServiceBinding> bindingsToDelete = controllerClient.getAppBindings(applicationGuid);
        context.setVariable(Variables.CLOUD_SERVICE_BINDINGS_TO_DELETE, bindingsToDelete);
        DynamicSecureSerialization dynamicSecureSerialization = SecureLoggingUtil.getDynamicSecureSerialization(context);
        getStepLogger().debug(Messages.EXISTING_SERVICE_BINDINGS, dynamicSecureSerialization.toJson(bindingsToDelete));
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_WHILE_DETERMINING_SERVICE_BINDINGS_TO_DELETE;
    }
}
