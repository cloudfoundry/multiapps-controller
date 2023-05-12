package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Named;

import org.apache.commons.collections4.ListUtils;
import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ServiceUtil;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ServiceCredentialBindingOperation;

@Named("determineServiceKeysToRecreateStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetermineServiceKeysToRecreateStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) throws Exception {
        CloudServiceInstanceExtended serviceToProcess = context.getVariable(Variables.SERVICE_TO_PROCESS);
        Map<String, List<CloudServiceKey>> serviceKeysMap = context.getVariable(Variables.SERVICE_KEYS_TO_CREATE);
        List<CloudServiceKey> serviceKeys = serviceKeysMap.get(serviceToProcess.getResourceName());
        CloudControllerClient controllerClient = context.getControllerClient();
        List<CloudServiceKey> existingServiceKeys = ServiceUtil.getExistingServiceKeys(controllerClient, serviceToProcess, getStepLogger());
        if (existingServiceKeys == null) {
            context.setVariable(Variables.CLOUD_SERVICE_KEYS_TO_DELETE, Collections.emptyList());
            context.setVariable(Variables.CLOUD_SERVICE_KEYS_TO_CREATE, Collections.emptyList());
            return StepPhase.DONE;
        }
        return calculateServiceKeyOperations(context, serviceKeys, existingServiceKeys);
    }

    private StepPhase calculateServiceKeyOperations(ProcessContext context, List<CloudServiceKey> serviceKeys,
                                                    List<CloudServiceKey> existingServiceKeys) {

        List<CloudServiceKey> serviceKeysToCreate = new ArrayList<>();
        List<CloudServiceKey> existingUnmodifiedServiceKeys = new ArrayList<>();
        List<CloudServiceKey> existingKeysWithDifferentMetadata = new ArrayList<>();
        List<CloudServiceKey> modifiedServiceKeys = new ArrayList<>();
        List<CloudServiceKey> serviceKeysInFailedState = new ArrayList<>();

        for (CloudServiceKey key : serviceKeys) {
            CloudServiceKey existingKey = getWithName(existingServiceKeys, key.getName());
            if (existingKey == null) {
                serviceKeysToCreate.add(key);
            } else if (isServiceKeyStateNotSucceeded(existingKey)) {
                serviceKeysInFailedState.add(key);
            } else if (areServiceKeysEqual(key, existingKey)) {
                if (Objects.equals(key.getV3Metadata(), existingKey.getV3Metadata())) {
                    existingUnmodifiedServiceKeys.add(key);
                } else {
                    existingKeysWithDifferentMetadata.add(ImmutableCloudServiceKey.copyOf(existingKey)
                                                                                  .withV3Metadata(key.getV3Metadata()));
                }
            } else {
                modifiedServiceKeys.add(key);
            }
        }

        if (StepsUtil.canDeleteServiceKeys(context)) {
            logRecreationDebugMessages(modifiedServiceKeys, serviceKeysInFailedState);

            List<CloudServiceKey> serviceKeysToRecreate = ListUtils.union(modifiedServiceKeys, serviceKeysInFailedState);
            // TODO: uncomment when implementing custom recreate attributes e.g. 'recreate-keys' and 'skip-recreate'
            // serviceKeysToRecreate.addAll(existingUnmodifiedServiceKeys);

            serviceKeysToCreate.addAll(serviceKeysToRecreate);
            context.setVariable(Variables.CLOUD_SERVICE_KEYS_TO_DELETE, serviceKeysToRecreate);
        } else {
            logCannotDeleteWarnMessages(modifiedServiceKeys, serviceKeysInFailedState);

            context.setVariable(Variables.CLOUD_SERVICE_KEYS_TO_DELETE, Collections.emptyList());
        }

        context.setVariable(Variables.CLOUD_SERVICE_KEYS_TO_CREATE, serviceKeysToCreate);
        context.setVariable(Variables.CLOUD_SERVICE_KEYS_TO_UPDATE_METADATA, existingKeysWithDifferentMetadata);
        getStepLogger().debug(Messages.SERVICE_KEYS_SCHEDULED_FOR_CREATION_0, JsonUtil.toJson(serviceKeysToCreate));
        return StepPhase.DONE;
    }

    private boolean isServiceKeyStateNotSucceeded(CloudServiceKey existingKey) {
        return existingKey.getServiceKeyOperation()
                          .getState() != ServiceCredentialBindingOperation.State.SUCCEEDED;
    }

    private boolean areServiceKeysEqual(CloudServiceKey key1, CloudServiceKey key2) {
        return Objects.equals(key1.getCredentials(), key2.getCredentials()) && Objects.equals(key1.getName(), key2.getName());
    }

    public static boolean isMetadataEqual(Metadata metadataA, Metadata metadataB) {
        if (metadataA == null || metadataB == null) {
            return metadataA == null && metadataB == null;
        }

        return Objects.equals(metadataA.getAnnotations(), metadataB.getAnnotations())
            && Objects.equals(metadataA.getLabels(), metadataB.getLabels());
    }

    private CloudServiceKey getWithName(List<CloudServiceKey> serviceKeys, String name) {
        return serviceKeys.stream()
                          .filter(key -> key.getName()
                                            .equals(name))
                          .findAny()
                          .orElse(null);
    }

    private void logRecreationDebugMessages(List<CloudServiceKey> modifiedServiceKeys, List<CloudServiceKey> serviceKeysInFailedState) {
        getStepLogger().debug(Messages.SERVICE_KEYS_SCHEDULED_FOR_RECREATION_MODIFICATION_0, JsonUtil.toJson(modifiedServiceKeys));
        getStepLogger().debug(Messages.SERVICE_KEYS_SCHEDULED_FOR_RECREATION_STATE_0, JsonUtil.toJson(serviceKeysInFailedState));
    }

    private void logCannotDeleteWarnMessages(List<CloudServiceKey> modifiedServiceKeys, List<CloudServiceKey> serviceKeysInFailedState) {
        modifiedServiceKeys.forEach(this::logWillNotUpdateMessage);
        serviceKeysInFailedState.forEach(this::logWillNotUpdateMessage);
    }

    private void logWillNotRecreateMessage(CloudServiceKey key) {
        getStepLogger().warn(Messages.WILL_NOT_RECREATE_SERVICE_KEY, key.getName(), key.getServiceInstance()
                                                                                       .getName());
    }

    private void logWillNotUpdateMessage(CloudServiceKey key) {
        getStepLogger().warn(Messages.WILL_NOT_UPDATE_SERVICE_KEY, key.getName(), key.getServiceInstance()
                                                                                     .getName());
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_WHILE_DETERMINING_SERVICE_KEYS_TO_RECREATE;
    }

}
