package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;
import java.util.function.Consumer;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudAsyncJob;

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
        return serviceBindingJob -> context.getStepLogger()
                                           .error(Messages.ASYNC_OPERATION_FOR_SERVICE_BINDING_FAILED_WITH, app.getName(),
                                                  serviceInstanceName, serviceBindingJob.getErrors());
    }

    @Override
    protected Consumer<CloudAsyncJob> getOnErrorHandlerForOptionalResource(ProcessContext context) {
        CloudApplication app = context.getVariable(Variables.APP_TO_PROCESS);
        String serviceInstanceName = context.getVariable(Variables.SERVICE_TO_UNBIND_BIND);
        return serviceBindingJob -> context.getStepLogger()
                                           .warnWithoutProgressMessage(Messages.ASYNC_OPERATION_FOR_SERVICE_BINDING_FOR_OPTIONAL_SERVICE_FAILED_WITH,
                                                                        app.getName(), serviceInstanceName, serviceBindingJob.getErrors());
    }

    @Override
    public String getPollingErrorMessage(ProcessContext context) {
        return Messages.ERROR_POLLING_ASYNC_SERVICE_BINDING;
    }

}
