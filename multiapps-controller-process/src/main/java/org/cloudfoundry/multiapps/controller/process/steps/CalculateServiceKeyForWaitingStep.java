package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ServiceUtil;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ServiceCredentialBindingOperation;

@Named("calculateServiceKeyForWaitingStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CalculateServiceKeyForWaitingStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) throws Exception {
        CloudServiceInstanceExtended serviceToProcess = context.getVariable(Variables.SERVICE_TO_PROCESS);
        CloudControllerClient controllerClient = context.getControllerClient();
        List<CloudServiceKey> existingServiceKeys = ServiceUtil.getExistingServiceKeys(controllerClient, serviceToProcess, getStepLogger());
        List<CloudServiceKey> serviceKeysInProgress = getServiceKeysInProgress(existingServiceKeys);
        getStepLogger().debug(Messages.SERVICE_KEYS_SCHEDULED_FOR_WAITING_0, SecureSerialization.toJson(serviceKeysInProgress));
        context.setVariable(Variables.CLOUD_SERVICE_KEYS_FOR_WAITING, serviceKeysInProgress);
        return StepPhase.DONE;
    }

    private List<CloudServiceKey> getServiceKeysInProgress(List<CloudServiceKey> existingServiceKeys) {
        if (existingServiceKeys == null) {
            return Collections.emptyList();
        }
        return existingServiceKeys.stream()
                                  .filter(key -> key.getServiceKeyOperation()
                                                    .getState() == ServiceCredentialBindingOperation.State.IN_PROGRESS
                                      || key.getServiceKeyOperation()
                                            .getState() == ServiceCredentialBindingOperation.State.INITIAL)
                                  .collect(Collectors.toList());
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_WHILE_CALCULATING_SERVICE_KEYS_FOR_WAITING;
    }

}
