package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;

@Named("detachServicesFromMtaStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetachServicesFromMtaStep extends ClearMtaMetadataBaseStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.DETACHING_SERVICES_FROM_MTA);
        List<String> serviceNamesToDetachFromMta = context.getVariable(Variables.SERVICES_TO_DELETE);
        CloudControllerClient client = context.getControllerClient();
        List<CloudServiceInstance> servicesToDetachFromMta = client.getServiceInstancesWithoutAuxiliaryContentByNames(serviceNamesToDetachFromMta);
        servicesToDetachFromMta.forEach(serviceToDetach -> deleteMtaMetadataFromService(client, serviceToDetach));
        getStepLogger().debug(Messages.SERVICES_DETACHED_FROM_MTA);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_DETACHING_SERVICES_FROM_MTA;
    }

}
