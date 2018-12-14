package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.XsCloudControllerClient;
import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Component("stageAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class StageAppStep extends TimeoutAsyncFlowableStep {

    @Inject
    protected RecentLogsRetriever recentLogsRetriever;

    @Override
    protected String getIndexVariable() {
        return Constants.VAR_MODULES_INDEX;
    }

    @Override
    protected StepPhase executeAsyncStep(ExecutionWrapper execution) {
        CloudApplication app = StepsUtil.getApp(execution.getContext());
        try {
            XsCloudControllerClient xsClient = execution.getXsControllerClient();

            return stageApp(execution.getContext(), xsClient, app);
        } catch (CloudOperationException coe) {
            CloudControllerException e = new CloudControllerException(coe);
            getStepLogger().error(e, Messages.ERROR_STAGING_APP_1, app.getName());
            throw e;
        }
    }

    private StepPhase stageApp(DelegateExecution context, XsCloudControllerClient xsClient, CloudApplication app) {
        getStepLogger().info(Messages.STAGING_APP, app.getName());
        StartingInfo startingInfo = xsClient.stageApplication(app.getName());
        StepsUtil.setStartingInfo(context, startingInfo);
        context.setVariable(Constants.VAR_START_TIME, System.currentTimeMillis());
        context.setVariable(Constants.VAR_OFFSET, 0);

        return StepPhase.POLL;
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ExecutionWrapper execution) {
        return Arrays.asList(new PollStageAppStatusExecution(recentLogsRetriever));
    }

    @Override
    public Integer getTimeout(DelegateExecution context) {
        return StepsUtil.getVariableOrDefault(context, Constants.PARAM_START_TIMEOUT, Constants.DEFAULT_START_TIMEOUT);
    }

}
