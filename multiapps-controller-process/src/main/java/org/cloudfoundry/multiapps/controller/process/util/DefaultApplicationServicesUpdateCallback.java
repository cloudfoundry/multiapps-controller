package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.steps.StepsUtil;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.ApplicationServicesUpdateCallback;
import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;

public class DefaultApplicationServicesUpdateCallback implements ApplicationServicesUpdateCallback {
    private final ProcessContext context;
    private final CloudControllerClient controllerClient;

    public DefaultApplicationServicesUpdateCallback(ProcessContext context, CloudControllerClient controllerClient) {
        this.context = context;
        this.controllerClient = controllerClient;
    }

    @Override
    public void onError(CloudOperationException e, String applicationName, String serviceInstanceName) {
        if (e.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
            UUID applicationGuid = controllerClient.getApplicationGuid(applicationName);
            UUID serviceInstanceGuid = controllerClient.getRequiredServiceInstanceGuid(serviceInstanceName);
            CloudServiceBinding serviceBinding = controllerClient.getServiceBindingForApplication(applicationGuid, serviceInstanceGuid);
            if (serviceBinding != null) {
                context.getStepLogger()
                       .warnWithoutProgressMessage(e, Messages.SERVICE_BINDING_BETWEEN_SERVICE_0_AND_APP_1_ALREADY_CREATED,
                                                   serviceInstanceName, applicationName);
                context.setVariable(Variables.USE_LAST_OPERATION_FOR_SERVICE_BINDING_CREATION, true);
                return;
            }
        }
        List<CloudServiceInstanceExtended> servicesToBind = context.getVariable(Variables.SERVICES_TO_BIND);
        if (StepsUtil.isServiceOptional(servicesToBind, serviceInstanceName)) {
            context.getStepLogger()
                   .warn(e, Messages.COULD_NOT_BIND_OPTIONAL_SERVICE_TO_APP, serviceInstanceName, applicationName);
            return;
        }
        throw new SLException(e, Messages.COULD_NOT_BIND_SERVICE_TO_APP, serviceInstanceName, applicationName, e.getMessage());
    }

}
