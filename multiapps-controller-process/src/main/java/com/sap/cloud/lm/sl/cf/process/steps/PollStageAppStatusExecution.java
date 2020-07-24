package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.UUID;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.PackageState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLoggerProvider;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationStager;
import com.sap.cloud.lm.sl.cf.process.util.StagingState;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

public class PollStageAppStatusExecution implements AsyncExecution {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollStageAppStatusExecution.class);

    private final RecentLogsRetriever recentLogsRetriever;
    private final ApplicationStager applicationStager;

    public PollStageAppStatusExecution(RecentLogsRetriever recentLogsRetriever, ApplicationStager applicationStager) {
        this.recentLogsRetriever = recentLogsRetriever;
        this.applicationStager = applicationStager;
    }

    @Override
    public AsyncExecutionState execute(ProcessContext context) {
        CloudApplication application = context.getVariable(Variables.APP_TO_PROCESS);
        CloudControllerClient client = context.getControllerClient();
        StepLogger stepLogger = context.getStepLogger();
        stepLogger.debug(Messages.CHECKING_APP_STATUS, application.getName());

        StagingState state = applicationStager.getStagingState();
        stepLogger.debug(Messages.APP_STAGING_STATUS, application.getName(), state.getState());

        ProcessLoggerProvider processLoggerProvider = stepLogger.getProcessLoggerProvider();
        StepsUtil.saveAppLogs(context.getExecution(), client, recentLogsRetriever, application, LOGGER, processLoggerProvider);

        if (state.getState() != PackageState.STAGED) {
            return checkStagingState(context.getStepLogger(), application, state);
        }
        bindDropletToApplication(client, application);
        stepLogger.info(Messages.APP_STAGED, application.getName());
        return AsyncExecutionState.FINISHED;
    }

    @Override
    public String getPollingErrorMessage(ProcessContext context) {
        CloudApplication application = context.getVariable(Variables.APP_TO_PROCESS);
        return MessageFormat.format(Messages.ERROR_STAGING_APP_0, application.getName());
    }

    private AsyncExecutionState checkStagingState(StepLogger stepLogger, CloudApplication application, StagingState state) {
        if (state.getState() == PackageState.FAILED) {
            stepLogger.error(Messages.ERROR_STAGING_APP_0_DESCRIPTION_1, application.getName(), state.getError());
            return AsyncExecutionState.ERROR;
        }
        return AsyncExecutionState.RUNNING;
    }

    private void bindDropletToApplication(CloudControllerClient client, CloudApplication application) {
        UUID applicationGuid = client.getApplication(application.getName())
                                     .getMetadata()
                                     .getGuid();
        applicationStager.bindDropletToApplication(applicationGuid);
    }

}
