package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationStager;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationStagerFactory;

@Component("stageAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class StageAppStep extends TimeoutAsyncFlowableStep {

    @Inject
    protected RecentLogsRetriever recentLogsRetriever;

    @Override
    protected String getIndexVariable() {
        return Constants.VAR_MODULES_INDEX;
    }
    
    @Autowired
    private ApplicationConfiguration configuration;

    @Override
    protected StepPhase executeAsyncStep(ExecutionWrapper execution) {
        CloudApplication app = StepsUtil.getApp(execution.getContext());
        try {
            ApplicationStager applicationStager = ApplicationStagerFactory.createApplicationStager(configuration.getPlatformType());

            return applicationStager.stageApp(execution.getContext(), execution.getControllerClient(), app, getStepLogger());
        } catch (CloudOperationException coe) {
            CloudControllerException e = new CloudControllerException(coe);
            getStepLogger().error(e, Messages.ERROR_STAGING_APP_1, app.getName());
            throw e;
        }
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ExecutionWrapper execution) {
        return Arrays.asList(new PollStageAppStatusExecution(recentLogsRetriever,
            ApplicationStagerFactory.createApplicationStager(configuration.getPlatformType())));
    }

    @Override
    public Integer getTimeout(DelegateExecution context) {
        return StepsUtil.getVariableOrDefault(context, Constants.PARAM_START_TIMEOUT, Constants.DEFAULT_START_TIMEOUT);
    }

}
