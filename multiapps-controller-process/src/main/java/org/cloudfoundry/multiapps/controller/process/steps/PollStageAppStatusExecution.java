package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.core.cf.clients.RecentLogsRetriever;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLoggerProvider;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ApplicationStager;
import org.cloudfoundry.multiapps.controller.process.util.StagingState;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.PackageState;

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
        StepsUtil.saveAppLogs(context, client, recentLogsRetriever, application, LOGGER, processLoggerProvider);

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
