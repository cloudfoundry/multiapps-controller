package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;

import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceBinding;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceCredentialBindingOperation;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

public class PollServiceBindingOrKeyOperationExecution implements AsyncExecution {

    @Override
    public AsyncExecutionState execute(ProcessContext context) {
        CloudControllerClient controllerClient = context.getControllerClient();
        String serviceName = context.getVariable(Variables.SERVICE_WITH_BIND_IN_PROGRESS);
        CloudServiceInstance service = controllerClient.getServiceInstance(serviceName);

        List<CloudServiceBinding> serviceBindings = controllerClient.getServiceAppBindings(service.getGuid());
        List<CloudServiceKey> serviceKeys = controllerClient.getServiceKeys(serviceName);

        if (doesServiceHaveBindingWithStateInProgress(serviceBindings) || doesServiceHaveKeysWithStateInProgress(serviceKeys)) {
            return AsyncExecutionState.RUNNING;
        }

        context.setVariable(Variables.WAS_SERVICE_BINDING_KEY_OPERATION_ALREADY_DONE, true);
        context.setVariable(Variables.IS_SERVICE_BINDING_KEY_OPERATION_IN_PROGRESS, false);
        return AsyncExecutionState.FINISHED;
    }

    private boolean doesServiceHaveBindingWithStateInProgress(List<CloudServiceBinding> serviceBindings) {
        return serviceBindings.stream()
                              .anyMatch(serviceBinding -> serviceBinding.getServiceBindingOperation()
                                                                        .getState()
                                                                        .equals(ServiceCredentialBindingOperation.State.IN_PROGRESS));
    }

    private boolean doesServiceHaveKeysWithStateInProgress(List<CloudServiceKey> serviceKeys) {
        return serviceKeys.stream()
                          .anyMatch(serviceKey -> serviceKey.getServiceKeyOperation()
                                                            .getState()
                                                            .equals(ServiceCredentialBindingOperation.State.IN_PROGRESS));
    }

    @Override
    public String getPollingErrorMessage(ProcessContext context) {
        return Messages.ERROR_MONITORING_OPERATION_OF_BINDING_OR_KEY_OF_SERVICE;
    }
}
