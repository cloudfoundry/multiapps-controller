package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.ServiceBindingOperation;

@Named("checkServiceBindingOperationStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CheckServiceBindingOperationStep extends AsyncFlowableStep {

    @Override
    protected StepPhase executeAsyncStep(ProcessContext context) throws Exception {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        String serviceInstanceName = context.getVariable(Variables.SERVICE_TO_UNBIND_BIND);
        getStepLogger().debug(MessageFormat.format(Messages.CHECKING_FOR_SERVICE_BINDING_OPERATION_IN_PROGRESS_BETWEEN_APP_0_AND_SERVICE_INSTANCE_1,
                                                   app.getName(), serviceInstanceName));
        CloudControllerClient controllerClient = context.getControllerClient();
        CloudServiceBinding serviceBinding = controllerClient.getServiceBindingForApplication(app.getName(), serviceInstanceName);
        if (serviceBinding == null) {
            return StepPhase.DONE;
        }
        return checkServiceBindingOperationState(serviceBinding, context);
    }

    private StepPhase checkServiceBindingOperationState(CloudServiceBinding serviceBinding, ProcessContext context) {
        ServiceBindingOperation lastOperation = serviceBinding.getServiceBindingOperation();
        getStepLogger().debug(MessageFormat.format(Messages.SERVICE_BINDING_OPERATION_0_IS_IN_STATE_1, serviceBinding.getGuid(),
                                                   lastOperation.getState()));
        if (lastOperation.getState() == ServiceBindingOperation.State.FAILED) {
            getStepLogger().warnWithoutProgressMessage(MessageFormat.format(Messages.SERVICE_BINDING_0_EXISTS_IN_BROKEN_STATE_WILL_BE_RECREATED,
                                                                            serviceBinding.getGuid()));
            context.setVariable(Variables.IS_SERVICE_BINDING_IN_FAILED_STATE, true);
            return StepPhase.DONE;
        }
        if (lastOperation.getState() == ServiceBindingOperation.State.SUCCEEDED) {
            return StepPhase.DONE;
        }
        return StepPhase.POLL;
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        return List.of(new PollServiceBindingLastOperationExecution());
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        String serviceInstanceName = context.getVariable(Variables.SERVICE_TO_UNBIND_BIND);
        return MessageFormat.format(Messages.ERROR_WHILE_CHECKING_SERVICE_BINDING_OPERATIONS_BETWEEN_APP_0_AND_SERVICE_INSTANCE_1,
                                    app.getName(), serviceInstanceName);
    }
}
