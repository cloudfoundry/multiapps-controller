package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.text.MessageFormat;
import java.util.UUID;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.PackageState;
import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLoggerProvider;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationStager;
import com.sap.cloud.lm.sl.cf.process.util.StagingState;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;

public class PollStageAppStatusExecution implements AsyncExecution {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollStageAppStatusExecution.class);

    private final RecentLogsRetriever recentLogsRetriever;
    private final ApplicationStager applicationStager;

    public PollStageAppStatusExecution(RecentLogsRetriever recentLogsRetriever, ApplicationStager applicationStager) {
        this.recentLogsRetriever = recentLogsRetriever;
        this.applicationStager = applicationStager;
    }

    @Override
    public AsyncExecutionState execute(ExecutionWrapper execution) {
        CloudApplication application = StepsUtil.getApp(execution.getContext());
        CloudControllerClient client = execution.getControllerClient();
        StepLogger stepLogger = execution.getStepLogger();
        stepLogger.debug(Messages.CHECKING_APP_STATUS, application.getName());

        StagingState state = applicationStager.getStagingState(execution.getContext());
        stepLogger.debug(Messages.APP_STAGING_STATUS, application.getName(), state.getState());

        ProcessLoggerProvider processLoggerProvider = stepLogger.getProcessLoggerProvider();
        StepsUtil.saveAppLogs(execution.getContext(), client, recentLogsRetriever, application, LOGGER, processLoggerProvider);

        if (!state.getState()
                  .equals(PackageState.STAGED)) {
            return checkStagingState(execution.getStepLogger(), application, state);
        }
        bindDropletToApplication(execution.getContext(), application, client);
        stepLogger.info(Messages.APP_STAGED, application.getName());
        return AsyncExecutionState.FINISHED;
    }

    @Override
    public String getPollingErrorMessage(ExecutionWrapper execution) {
        CloudApplication app = StepsUtil.getApp(execution.getContext());
        return MessageFormat.format(Messages.ERROR_STAGING_APP_1, app.getName());
    }

    private AsyncExecutionState checkStagingState(StepLogger stepLogger, CloudApplication app, StagingState state) {
        if (state.getState()
                 .equals(PackageState.FAILED)) {
            stepLogger.error(format(Messages.ERROR_STAGING_APP_2, app.getName()));
            stepLogger.error(state.getError());
            return AsyncExecutionState.ERROR;
        }
        return AsyncExecutionState.RUNNING;
    }

    private void bindDropletToApplication(DelegateExecution context, CloudApplication application, CloudControllerClient client) {
        UUID appId = client.getApplication(application.getName())
                           .getMetadata()
                           .getGuid();
        applicationStager.bindDropletToApplication(context, appId);
    }
}
