package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;

@Named("deleteServiceMtaMetadataStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteServiceMtaMetadataStep extends ClearMtaMetadataBaseStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) throws Exception {
        String serviceInstanceToDelete = context.getVariable(Variables.SERVICE_TO_DELETE);
        CloudControllerClient controllerClient = context.getControllerClient();
        CloudServiceInstance serviceInstance = controllerClient.getServiceInstanceWithoutAuxiliaryContent(serviceInstanceToDelete);
        deleteMtaMetadataFromService(controllerClient, serviceInstance);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        String serviceInstanceToDelete = context.getVariable(Variables.SERVICE_TO_DELETE);
        return MessageFormat.format(Messages.ERROR_WHILE_DELETING_SERVICE_INSTANCE_METADATA_0, serviceInstanceToDelete);
    }
}
