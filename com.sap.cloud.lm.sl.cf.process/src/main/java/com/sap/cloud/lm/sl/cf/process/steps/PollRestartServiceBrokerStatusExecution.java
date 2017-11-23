package com.sap.cloud.lm.sl.cf.process.steps;

import org.activiti.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.core.util.Configuration;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("pollServiceBrokerRestartStatus")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PollRestartServiceBrokerStatusExecution extends PollStartAppStatusExecution {

    public PollRestartServiceBrokerStatusExecution(RecentLogsRetriever recentLogsRetriever, Configuration configuration) {
        super(recentLogsRetriever, configuration);
    }

    @Override
    public AsyncExecutionState execute(ExecutionWrapper execution) throws SLException {
        try {
            AsyncExecutionState status = super.execute(execution);
            if (status.equals(AsyncExecutionState.ERROR)) {
                status = AsyncExecutionState.FINISHED;
            }
            return status;
        } catch (SLException e) {
            execution.getStepLogger().warn(e, Messages.FAILED_SERVICE_BROKER_START, getAppToPoll(execution.getContext()).getName());
            return AsyncExecutionState.FINISHED;
        }
    }

    @Override
    protected CloudApplicationExtended getAppToPoll(DelegateExecution context) {
        return StepsUtil.getServiceBrokerSubscriberToRestart(context);
    }

    @Override
    protected void onError(ExecutionWrapper execution, String message, Exception e) {
        execution.getStepLogger().warn(e, message);
    }

    @Override
    protected void onError(ExecutionWrapper execution, String message) {
        execution.getStepLogger().warn(message);
    }

}
