package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationStager;

@Component("stageAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class StageAppStep extends TimeoutAsyncFlowableStep {

    @Inject
    protected RecentLogsRetriever recentLogsRetriever;

    @Override
    protected StepPhase executeAsyncStep(ExecutionWrapper execution) {
        CloudApplication app = StepsUtil.getApp(execution.getContext());
        ApplicationStager applicationStager = new ApplicationStager();
        return applicationStager.stageApp(execution.getContext(), execution.getControllerClient(), app, getStepLogger());
    }

    @Override
    protected void onStepError(DelegateExecution context, Exception e) throws Exception {
        getStepLogger().error(e, Messages.ERROR_STAGING_APP_1, StepsUtil.getApp(context)
            .getName());
        throw e;
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ExecutionWrapper execution) {
        recentLogsRetriever.setFailSafe(true);
        return Arrays.asList(new PollStageAppStatusExecution(recentLogsRetriever, new ApplicationStager()));
    }

    @Override
    public Integer getTimeout(DelegateExecution context) {
        return StepsUtil.getInteger(context, Constants.PARAM_START_TIMEOUT, Constants.DEFAULT_START_TIMEOUT);
    }

}
