package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.slp.model.LoopStepMetadata;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("prepareToRestartServiceBrokersStep")
public class PrepareToRestartServiceBrokersStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareToRestartServiceBrokersStep.class);

    public static StepMetadata getMetadata() {
        return LoopStepMetadata.builder().id("prepareToRestartServiceBrokersTask").displayName(
            "Prepare To Restart Service Brokers").description("Prepare To Restart Service Brokers").children(
                RestartServiceBrokerSubscriberStep.getMetadata(), UpdateServiceBrokerStep.getMetadata()).countVariable(
                    Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS_COUNT).build();
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) {
        logActivitiTask(context, LOGGER);

        List<CloudApplicationExtended> serviceBrokersToRestart = StepsUtil.getServiceBrokerSubscribersToRestart(context);
        prepareServiceBrokersToRestart(context, serviceBrokersToRestart);
        return ExecutionStatus.SUCCESS;

    }

    private void prepareServiceBrokersToRestart(DelegateExecution context, List<CloudApplicationExtended> serviceBrokersToRestart) {
        context.setVariable(Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS_COUNT, serviceBrokersToRestart.size());
        context.setVariable(Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS_INDEX, 0);
        context.setVariable(Constants.VAR_INDEX_VARIABLE_NAME, Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS_INDEX);
        context.setVariable(Constants.VAR_CONTROLLER_POLLING_INTERVAL, ConfigurationUtil.getControllerPollingInterval());
    }
}
