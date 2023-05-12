package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaServiceKey;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;

@Named("detachServiceKeysFromMtaStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetachServiceKeysFromMtaStep extends ClearMtaMetadataBaseStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.DETACHING_SERVICE_KEYS_FROM_MTA);
        List<DeployedMtaServiceKey> serviceKeysToDetachFromMta = context.getVariable(Variables.DEPLOYED_MTA_SERVICE_KEYS);
        CloudControllerClient client = context.getControllerClient();
        serviceKeysToDetachFromMta.forEach(serviceKey -> deleteMtaMetadataFromServiceKey(client, serviceKey));
        getStepLogger().debug(Messages.SERVICE_KEYS_DETACHED_FROM_MTA);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_DETACHING_SERVICES_FROM_MTA;
    }

}
