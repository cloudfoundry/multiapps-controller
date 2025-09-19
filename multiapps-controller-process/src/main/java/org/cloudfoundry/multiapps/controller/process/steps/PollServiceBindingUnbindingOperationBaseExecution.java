package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;
import java.util.function.Consumer;

import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudAsyncJob;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

public abstract class PollServiceBindingUnbindingOperationBaseExecution extends PollOperationBaseExecution {

    @Override
    protected boolean isOptional(ProcessContext context) {
        List<CloudServiceInstanceExtended> servicesToBind = context.getVariable(Variables.SERVICES_TO_BIND);
        String serviceInstanceName = context.getVariable(Variables.SERVICE_TO_UNBIND_BIND);
        return StepsUtil.isServiceOptional(servicesToBind, serviceInstanceName);
    }

    @Override
    protected Consumer<CloudAsyncJob> getInProgressHandler(ProcessContext context) {
        return serviceBindingJob -> context.getStepLogger()
                                           .debug(Messages.ASYNC_OPERATION_SERVICE_BINDING_IN_STATE_WITH_WARNINGS,
                                                  serviceBindingJob.getState(), serviceBindingJob.getWarnings());
    }

    @Override
    protected Consumer<CloudAsyncJob> getOnCompleteHandler(ProcessContext context) {
        return serviceBindingJob -> context.getStepLogger()
                                           .debug(Messages.ASYNC_OPERATION_FOR_SERVICE_BINDING_FINISHED, serviceBindingJob.getGuid());
    }

    @Override
    protected Consumer<CloudAsyncJob> getOnErrorHandler(ProcessContext context) {
        CloudApplication app = context.getVariable(Variables.APP_TO_PROCESS);
        String serviceInstanceName = context.getVariable(Variables.SERVICE_TO_UNBIND_BIND);

        CloudControllerClient client = context.getControllerClient();
        CloudServiceInstance serviceInstance = client.getServiceInstance(serviceInstanceName, false);

        return serviceBindingJob -> context.getStepLogger()
                                           .error(buildErrorMessage(app, serviceInstance, serviceInstanceName, serviceBindingJob));

    }

    private String buildErrorMessage(CloudApplication app, CloudServiceInstance serviceInstance, String serviceInstanceName,
                                     CloudAsyncJob serviceBindingJob) {

        if (serviceInstance == null) {
            return MessageFormat.format(Messages.ASYNC_OPERATION_FOR_SERVICE_BINDING_FAILED_INSTANCE_MISSING, app.getName(),
                                        serviceInstanceName, serviceBindingJob.getErrors());
        } else if (serviceInstance.isUserProvided()) {
            return MessageFormat.format(Messages.ASYNC_OPERATION_FOR_USER_PROVIDED_SERVICE_BINDING_FAILED_WITH, app.getName(),
                                        serviceInstanceName, serviceBindingJob.getErrors());
        } else {
            return MessageFormat.format(Messages.ASYNC_OPERATION_FOR_SERVICE_BINDING_FAILED_WITH, app.getName(), serviceInstanceName,
                                        serviceInstance.getLabel(), serviceInstance.getPlan(),
                                        serviceBindingJob.getErrors());
        }
    }

    @Override
    protected Consumer<CloudAsyncJob> getOnErrorHandlerForOptionalResource(ProcessContext context) {
        CloudApplication app = context.getVariable(Variables.APP_TO_PROCESS);
        String serviceInstanceName = context.getVariable(Variables.SERVICE_TO_UNBIND_BIND);
        return serviceBindingJob -> context.getStepLogger()
                                           .warnWithoutProgressMessage(
                                               Messages.ASYNC_OPERATION_FOR_SERVICE_BINDING_FOR_OPTIONAL_SERVICE_FAILED_WITH, app.getName(),
                                               serviceInstanceName, serviceBindingJob.getErrors());
    }

    @Override
    public String getPollingErrorMessage(ProcessContext context) {
        return Messages.ERROR_POLLING_ASYNC_SERVICE_BINDING;
    }

}
