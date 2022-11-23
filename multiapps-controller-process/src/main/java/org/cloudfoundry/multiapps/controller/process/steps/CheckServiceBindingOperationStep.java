package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.ServiceCredentialBindingOperation;

@Named("checkServiceBindingOperationStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CheckServiceBindingOperationStep extends AsyncFlowableStep {

    @Override
    protected StepPhase executeAsyncStep(ProcessContext context) throws Exception {
        CloudServiceBinding serviceBinding = getServiceBindingForProcessing(context);
        if (serviceBinding == null) {
            return StepPhase.DONE;
        }
        return checkServiceBindingOperationState(serviceBinding, context);
    }

    private CloudServiceBinding getServiceBindingForProcessing(ProcessContext context) {
        CloudServiceBinding serviceBindingToDelete = context.getVariable(Variables.SERVICE_BINDING_TO_DELETE);
        if (serviceBindingToDelete != null) {
            getStepLogger().debug(Messages.SERVICE_BINDING_0_SCHEDULED_FOR_DELETION, serviceBindingToDelete.getGuid());
            return serviceBindingToDelete;
        }
        return getServiceBindingForAppAndServiceInstance(context);
    }

    private CloudServiceBinding getServiceBindingForAppAndServiceInstance(ProcessContext context) {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        String serviceInstanceName = context.getVariable(Variables.SERVICE_TO_UNBIND_BIND);
        getStepLogger().debug(Messages.CHECKING_FOR_SERVICE_BINDING_OPERATION_IN_PROGRESS_BETWEEN_APP_0_AND_SERVICE_INSTANCE_1,
                              app.getName(), serviceInstanceName);
        return getCloudServiceBinding(context, app, serviceInstanceName);
    }

    private CloudServiceBinding getCloudServiceBinding(ProcessContext context, CloudApplicationExtended app, String serviceInstanceName) {
        CloudControllerClient controllerClient = context.getControllerClient();
        try {
            UUID applicationGuid = controllerClient.getApplicationGuid(app.getName());
            UUID serviceInstanceGuid = controllerClient.getRequiredServiceInstanceGuid(serviceInstanceName);
            return controllerClient.getServiceBindingForApplication(applicationGuid, serviceInstanceGuid);
        } catch (CloudOperationException e) {
            List<CloudServiceInstanceExtended> servicesToBind = context.getVariable(Variables.SERVICES_TO_BIND);
            if (StepsUtil.isServiceOptional(servicesToBind, serviceInstanceName)) {
                getStepLogger().warnWithoutProgressMessage(e, Messages.CANNOT_RETRIEVE_OPTIONAL_SERVICE_BINDING_FOR_SERVICE_INSTANCE_0,
                                                           serviceInstanceName);
                return null;
            }
            throw e;
        }
    }

    private StepPhase checkServiceBindingOperationState(CloudServiceBinding serviceBinding, ProcessContext context) {
        ServiceCredentialBindingOperation lastOperation = serviceBinding.getServiceBindingOperation();
        getStepLogger().debug(Messages.SERVICE_BINDING_OPERATION_WITH_TYPE_IS_IN_STATE, serviceBinding.getGuid(), lastOperation.getType(),
                              lastOperation.getState());
        if (lastOperation.getState() == ServiceCredentialBindingOperation.State.FAILED) {
            getStepLogger().warnWithoutProgressMessage(Messages.SERVICE_BINDING_0_EXISTS_IN_BROKEN_STATE_WILL_BE_RECREATED,
                                                       serviceBinding.getGuid());
            context.setVariable(Variables.SHOULD_RECREATE_SERVICE_BINDING, true);
            return StepPhase.DONE;
        }
        if (lastOperation.getState() == ServiceCredentialBindingOperation.State.SUCCEEDED) {
            return StepPhase.DONE;
        }
        return StepPhase.POLL;
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        return List.of(new PollServiceBindingLastOperationFailSafeExecution());
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        String serviceInstanceName = context.getVariable(Variables.SERVICE_TO_UNBIND_BIND);
        return MessageFormat.format(Messages.ERROR_WHILE_CHECKING_SERVICE_BINDING_OPERATIONS_BETWEEN_APP_0_AND_SERVICE_INSTANCE_1,
                                    app.getName(), serviceInstanceName);
    }
}
