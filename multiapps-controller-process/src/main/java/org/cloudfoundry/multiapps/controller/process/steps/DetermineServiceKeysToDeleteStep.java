package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaServiceKey;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;

/**
 * 
 * This class and corresponding flowable tasks should be removed in a following release.
 *
 */
@Named("determineServiceKeysToDeleteStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetermineServiceKeysToDeleteStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) throws Exception {
        getStepLogger().debug(Messages.DELETING_OLD_SERVICE_KEYS);
        List<DeployedMtaServiceKey> serviceKeysToDelete = context.getVariable(Variables.SERVICE_KEYS_TO_DELETE);
        List<CloudServiceKey> cloudServiceKeys = new ArrayList<>(serviceKeysToDelete);
        getStepLogger().debug(Messages.DELETING_OLD_SERVICE_KEYS_FOR_SERVICE, cloudServiceKeys.stream()
                                                                                              .map(CloudServiceKey::getName)
                                                                                              .collect(Collectors.toList()));
        context.setVariable(Variables.CLOUD_SERVICE_KEYS_TO_DELETE, cloudServiceKeys);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_WHILE_DETERMINING_SERVICE_KEYS_TO_DELETE;
    }

}