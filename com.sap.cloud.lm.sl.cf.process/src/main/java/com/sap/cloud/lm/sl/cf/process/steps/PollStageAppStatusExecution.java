package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.text.MessageFormat;
import java.util.UUID;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.PackageState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLoggerProvider;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationStager;
import com.sap.cloud.lm.sl.cf.process.util.StagingState;
import com.sap.cloud.lm.sl.cf.process.util.StagingState.StagingLogs;
import com.sap.cloud.lm.sl.cf.process.util.XMLValueFilter;

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
        CloudApplication app = StepsUtil.getApp(execution.getContext());
        CloudControllerClient client = execution.getControllerClient();

        execution.getStepLogger()
                 .debug(Messages.CHECKING_APP_STATUS, app.getName());

        StagingState state = applicationStager.getStagingState(execution.getContext());
        execution.getStepLogger()
                 .debug(Messages.APP_STAGING_STATUS, app.getName(), state.getState());

        ProcessLoggerProvider processLoggerProvider = execution.getStepLogger()
                                                               .getProcessLoggerProvider();
        StepsUtil.saveAppLogs(execution.getContext(), client, recentLogsRetriever, app, LOGGER, processLoggerProvider);

        if (!state.getState()
                  .equals(PackageState.STAGED)) {
            setStagingLogs(state, execution);

            return checkStagingState(execution, app, state);
        }

        execution.getStepLogger()
                 .info(Messages.APP_STAGED, app.getName());

        UUID appId = client.getApplication(app.getName())
                           .getMetadata()
                           .getGuid();

        applicationStager.bindDropletToApp(execution.getContext(), appId);

        return AsyncExecutionState.FINISHED;
    }

    @Override
    public String getPollingErrorMessage(ExecutionWrapper execution) {
        CloudApplication app = StepsUtil.getApp(execution.getContext());
        return MessageFormat.format(Messages.ERROR_STAGING_APP_1, app.getName());
    }

    private AsyncExecutionState checkStagingState(ExecutionWrapper execution, CloudApplication app, StagingState state) {
        if (state.getState()
                 .equals(PackageState.FAILED)) {
            // Application staging failed
            String message = format(Messages.ERROR_STAGING_APP_2, app.getName());
            execution.getStepLogger()
                     .error(message);
            return AsyncExecutionState.ERROR;
        }
        // Application not staged yet, wait and try again unless it's a timeout.
        return AsyncExecutionState.RUNNING;
    }

    private void setStagingLogs(StagingState state, ExecutionWrapper execution) {
        if (state.getStagingLogs() != null) {
            StagingLogs logs = state.getStagingLogs();
            String stagingLogs = logs.getLogs();
            int offset = logs.getOffset();

            stagingLogs = new XMLValueFilter(stagingLogs).getFiltered();
            execution.getStepLogger()
                     .debug(stagingLogs);
            offset += stagingLogs.length();
            execution.getContext()
                     .setVariable(Constants.VAR_OFFSET, offset);
        }
    }

}
