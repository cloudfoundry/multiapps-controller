package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

@Named("prepareToRestartServiceBrokerSubscribersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PrepareToRestartServiceBrokerSubscribersStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        List<CloudApplication> serviceBrokersToRestart = context.getVariable(Variables.UPDATED_SERVICE_BROKER_SUBSCRIBERS);
        context.setVariable(Variables.UPDATED_SERVICE_BROKER_SUBSCRIBERS_COUNT, serviceBrokersToRestart.size());
        context.setVariable(Variables.UPDATED_SERVICE_BROKER_SUBSCRIBERS_INDEX, 0);
        context.setVariable(Variables.INDEX_VARIABLE_NAME, Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS_INDEX);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_PREPARING_TO_RESTART_SERVICE_BROKER_SUBSCRIBERS;
    }

}
