package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.DeletingServiceBindingOperationCallback;
import org.cloudfoundry.multiapps.controller.process.util.ServiceBindingPollingFactory;
import org.cloudfoundry.multiapps.controller.process.util.UnbindServiceFromApplicationCallback;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.ApplicationServicesUpdateCallback;
import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.ServiceBindingOperationCallback;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.ServiceCredentialBindingOperation;

@Named("unbindServiceFromApplicationStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UnbindServiceFromApplicationStep extends AsyncFlowableStep {

    @Override
    protected StepPhase executeAsyncStep(ProcessContext context) throws Exception {
        CloudServiceBinding serviceBindingToDelete = context.getVariable(Variables.SERVICE_BINDING_TO_DELETE);
        CloudControllerClient controllerClient = context.getControllerClient();
        if (serviceBindingToDelete != null) {
            return deleteServiceBinding(context,
                                        () -> controllerClient.deleteServiceBinding(serviceBindingToDelete.getGuid(),
                                                                                    getServiceBindingOperationCallback(context,
                                                                                                                       controllerClient)),
                                        () -> MessageFormat.format(Messages.DELETION_OF_SERVICE_BINDING_0_FINISHED,
                                                                   serviceBindingToDelete.getGuid()));
        }
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        String serviceInstanceName = context.getVariable(Variables.SERVICE_TO_UNBIND_BIND);
        getStepLogger().info(Messages.UNBINDING_SERVICE_INSTANCE_FROM_APP, serviceInstanceName, app.getName());
        return deleteServiceBinding(context,
                                    () -> controllerClient.unbindServiceInstance(app.getName(), serviceInstanceName,
                                                                                 getApplicationServicesUpdateCallback(context,
                                                                                                                      controllerClient)),
                                    () -> MessageFormat.format(Messages.UNBINDING_SERVICE_INSTANCE_FROM_APP_FINISHED, serviceInstanceName,
                                                               app.getName()));
    }

    private StepPhase deleteServiceBinding(ProcessContext context, Supplier<Optional<String>> serviceBindingJobSupplier,
                                           Supplier<String> messageSupplier) {
        Optional<String> jobId = serviceBindingJobSupplier.get();
        if (context.getVariable(Variables.USE_LAST_OPERATION_FOR_SERVICE_BINDING_DELETION)) {
            return StepPhase.POLL;
        }
        if (jobId.isEmpty()) {
            getStepLogger().infoWithoutProgressMessage(messageSupplier.get());
            return StepPhase.DONE;
        }
        context.setVariable(Variables.SERVICE_UNBINDING_JOB_ID, jobId.get());
        return StepPhase.POLL;
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        AsyncExecution pollingExecution = createServiceBindingPollingFactory(context).createPollingExecution();
        return List.of(pollingExecution);
    }

    private ServiceBindingPollingFactory createServiceBindingPollingFactory(ProcessContext context) {
        return new ServiceBindingPollingFactory(context, ServiceCredentialBindingOperation.Type.DELETE);
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        CloudServiceBinding serviceBindingToDelete = context.getVariable(Variables.SERVICE_BINDING_TO_DELETE);
        if (serviceBindingToDelete != null) {
            return MessageFormat.format(Messages.ERROR_WHILE_DELETING_SERVICE_BINDING_0, serviceBindingToDelete.getGuid());
        }
        return MessageFormat.format(Messages.ERROR_WHILE_UNBINDING_SERVICE_INSTANCE_FROM_APPLICATION,
                                    context.getVariable(Variables.SERVICE_TO_UNBIND_BIND), context.getVariable(Variables.APP_TO_PROCESS)
                                                                                                  .getName());
    }

    private ApplicationServicesUpdateCallback getApplicationServicesUpdateCallback(ProcessContext context,
                                                                                   CloudControllerClient controllerClient) {
        return new UnbindServiceFromApplicationCallback(context, controllerClient);
    }

    private ServiceBindingOperationCallback getServiceBindingOperationCallback(ProcessContext context,
                                                                               CloudControllerClient controllerClient) {
        return new DeletingServiceBindingOperationCallback(context, controllerClient);
    }

}
