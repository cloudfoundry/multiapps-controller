package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceCredentialBindingOperation;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.security.serialization.DynamicSecureSerialization;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerializationFactory;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ServiceUtil;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("calculateServiceKeyForWaitingStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CalculateServiceKeyForWaitingStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) throws Exception {
        CloudServiceInstanceExtended serviceToProcess = context.getVariable(Variables.SERVICE_TO_PROCESS);
        CloudControllerClient controllerClient = context.getControllerClient();
        List<CloudServiceKey> existingServiceKeys = ServiceUtil.getExistingServiceKeys(controllerClient, serviceToProcess, getStepLogger());
        List<CloudServiceKey> serviceKeysInProgress = getServiceKeysInProgress(existingServiceKeys);
        Set<String> secretParameters = context.getVariable(Variables.SECURE_EXTENSION_DESCRIPTOR_PARAMETER_NAMES);
        DynamicSecureSerialization dynamicSecureSerialization = SecureSerializationFactory.ofAdditionalValues(secretParameters);
        getStepLogger().debug(Messages.SERVICE_KEYS_SCHEDULED_FOR_WAITING_0, dynamicSecureSerialization.toJson(serviceKeysInProgress));
        context.setVariable(Variables.CLOUD_SERVICE_KEYS_FOR_WAITING, serviceKeysInProgress);
        return StepPhase.DONE;
    }

    private List<CloudServiceKey> getServiceKeysInProgress(List<CloudServiceKey> existingServiceKeys) {
        if (existingServiceKeys == null) {
            return Collections.emptyList();
        }
        return existingServiceKeys.stream()
                                  .filter(key -> key.getServiceKeyOperation()
                                                    .getState() == ServiceCredentialBindingOperation.State.IN_PROGRESS ||
                                      key.getServiceKeyOperation()
                                         .getState() == ServiceCredentialBindingOperation.State.INITIAL)
                                  .collect(Collectors.toList());
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_WHILE_CALCULATING_SERVICE_KEYS_FOR_WAITING;
    }

}
