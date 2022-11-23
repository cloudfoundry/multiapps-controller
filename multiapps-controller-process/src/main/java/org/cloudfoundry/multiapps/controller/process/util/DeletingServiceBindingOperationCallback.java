package org.cloudfoundry.multiapps.controller.process.util;

import java.util.UUID;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.ServiceBindingOperationCallback;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;

public class DeletingServiceBindingOperationCallback implements ServiceBindingOperationCallback {

    private final ProcessContext context;
    private final CloudControllerClient controllerClient;

    public DeletingServiceBindingOperationCallback(ProcessContext context, CloudControllerClient controllerClient) {
        this.context = context;
        this.controllerClient = controllerClient;
    }

    @Override
    public void onError(CloudOperationException e, UUID serviceBindingGuid) {
        if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
            context.getStepLogger()
                   .warnWithoutProgressMessage(e, Messages.SERVICE_BINDING_0_IS_ALREADY_DELETED, serviceBindingGuid);
            return;
        }
        if (e.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
            CloudServiceBinding serviceBinding = controllerClient.getServiceBinding(serviceBindingGuid);
            if (serviceBinding != null) {
                context.setVariable(Variables.USE_LAST_OPERATION_FOR_SERVICE_BINDING_DELETION, true);
                return;
            }
        }
        throw new SLException(e, Messages.ERROR_OCCURRED_WHILE_DELETING_SERVICE_BINDING_0, serviceBindingGuid);
    }

}
