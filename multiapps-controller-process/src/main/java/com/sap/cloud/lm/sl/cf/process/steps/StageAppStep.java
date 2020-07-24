package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationStager;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

@Named("stageAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class StageAppStep extends TimeoutAsyncFlowableStep {

    @Inject
    protected RecentLogsRetriever recentLogsRetriever;

    @Override
    protected StepPhase executeAsyncStep(ProcessContext context) {
        CloudApplication app = context.getVariable(Variables.APP_TO_PROCESS);
        ApplicationStager applicationStager = new ApplicationStager(context);
        return applicationStager.stageApp(app);
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format(Messages.ERROR_STAGING_APP_0, context.getVariable(Variables.APP_TO_PROCESS)
                                                                         .getName());
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        return Collections.singletonList(new PollStageAppStatusExecution(recentLogsRetriever, new ApplicationStager(context)));
    }

    @Override
    public Integer getTimeout(ProcessContext context) {
        return context.getVariable(Variables.START_TIMEOUT);
    }

}
