package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;
import java.util.UUID;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;

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
        getStepLogger().debug(Messages.EXISTING_SERVICE_BINDINGS, SecureSerialization.toJson(bindingsToDelete));
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_WHILE_DETERMINING_SERVICE_BINDINGS_TO_DELETE;
    }
}
