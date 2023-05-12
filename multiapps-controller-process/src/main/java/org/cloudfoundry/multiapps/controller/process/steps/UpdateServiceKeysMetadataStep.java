package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;

@Named("updateServiceKeysMetadataStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateServiceKeysMetadataStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) throws Exception {
        List<CloudServiceKey> serviceKeysToUpdate = context.getVariable(Variables.CLOUD_SERVICE_KEYS_TO_UPDATE_METADATA);
        if (serviceKeysToUpdate.isEmpty()) {
            return StepPhase.DONE;
        }

        getStepLogger().debug(Messages.SERVICE_KEYS_FOR_METADATA_UPDATE_0, serviceKeysToUpdate.stream()
                                                                                              .map(CloudServiceKey::getName)
                                                                                              .collect(Collectors.toList()));
        CloudControllerClient client = context.getControllerClient();
        serviceKeysToUpdate.forEach(serviceKey -> updateMtaMetadataForServiceKey(client, serviceKey));

        getStepLogger().debug(Messages.SERVICE_KEYS_METADATA_UPDATE_DONE);
        return StepPhase.DONE;
    }

    private void updateMtaMetadataForServiceKey(CloudControllerClient client, CloudServiceKey serviceKey) {
        getStepLogger().debug(MessageFormat.format(Messages.UPDATING_SERVICE_KEY_0_METADATA, serviceKey.getName()));

        try {
            client.updateServiceBindingMetadata(serviceKey.getGuid(), serviceKey.getV3Metadata());
        } catch (CloudOperationException e) {
            getStepLogger().errorWithoutProgressMessage(e, Messages.UPDATING_SERVICE_KEY_0_METADATA_FAILED, serviceKey.getName());
        }
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_WHILE_UPDATING_SERVICE_KEYS_METADATA;
    }

}
