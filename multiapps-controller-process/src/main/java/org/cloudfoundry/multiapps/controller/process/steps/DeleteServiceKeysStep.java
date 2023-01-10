package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaServiceKey;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;

@Named("deleteServiceKeysStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteServiceKeysStep extends SyncFlowableStep {

    // TODO: This step does not exist in the diagrams and it is only for 1 tact backwards compatibility. Delete after 1 tact!
    @Override
    protected StepPhase executeStep(ProcessContext context) throws Exception {
        getStepLogger().debug(Messages.DELETING_OLD_SERVICE_KEYS);

        CloudControllerClient client = context.getControllerClient();
        List<DeployedMtaServiceKey> serviceKeysToDelete = context.getVariable(Variables.SERVICE_KEYS_TO_DELETE);

        if (!serviceKeysToDelete.isEmpty()) {
            deleteServiceKeys(serviceKeysToDelete, client);
        }

        return StepPhase.DONE;
    }

    private void deleteServiceKeys(List<DeployedMtaServiceKey> serviceKeys, CloudControllerClient client) {
        getStepLogger().info(Messages.DELETING_OLD_SERVICE_KEYS_FOR_SERVICE, serviceKeys.stream()
                                                                                        .map(CloudServiceKey::getName)
                                                                                        .collect(Collectors.toList()));
        for (CloudServiceKey serviceKey : serviceKeys) {
            try {
                client.deleteServiceBindingSync(serviceKey.getGuid());
            } catch (CloudOperationException e) {
                getStepLogger().warn(e, "Service key deletion failed");
            }
        }
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return "Error while deleting service keys!";
    }

}
