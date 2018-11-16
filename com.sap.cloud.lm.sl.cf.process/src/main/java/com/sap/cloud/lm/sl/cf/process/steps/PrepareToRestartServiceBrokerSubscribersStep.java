package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.process.Constants;

@Component("prepareToRestartServiceBrokerSubscribersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PrepareToRestartServiceBrokerSubscribersStep extends SyncFlowableStep {

    @Inject
    private ApplicationConfiguration configuration;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        DelegateExecution context = execution.getContext();

        List<CloudApplication> serviceBrokersToRestart = StepsUtil.getServiceBrokerSubscribersToRestart(context);
        context.setVariable(Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS_COUNT, serviceBrokersToRestart.size());
        context.setVariable(Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS_INDEX, 0);
        context.setVariable(Constants.VAR_INDEX_VARIABLE_NAME, Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS_INDEX);
        context.setVariable(Constants.VAR_CONTROLLER_POLLING_INTERVAL, configuration.getControllerPollingInterval());
        return StepPhase.DONE;
    }

}
