package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;

@Named("prepareToRestartServiceBrokerSubscribersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PrepareToRestartServiceBrokerSubscribersStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        List<CloudApplication> serviceBrokersToRestart = context.getVariable(Variables.UPDATED_SERVICE_BROKER_SUBSCRIBERS);
        context.setVariable(Variables.UPDATED_SERVICE_BROKER_SUBSCRIBERS_COUNT, serviceBrokersToRestart.size());
        context.setVariable(Variables.UPDATED_SERVICE_BROKER_SUBSCRIBERS_INDEX, 0);
        context.setVariable(Variables.INDEX_VARIABLE_NAME, Variables.UPDATED_SERVICE_BROKER_SUBSCRIBERS_INDEX.getName());
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_PREPARING_TO_RESTART_SERVICE_BROKER_SUBSCRIBERS;
    }

}
