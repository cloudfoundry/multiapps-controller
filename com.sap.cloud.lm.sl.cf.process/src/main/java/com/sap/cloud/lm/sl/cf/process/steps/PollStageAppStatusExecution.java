package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStagingState;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ApplicationStagingStateGetter;
import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.XMLValueFilter;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.Pair;

public class PollStageAppStatusExecution implements AsyncExecution {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollStageAppStatusExecution.class);

    private RecentLogsRetriever recentLogsRetriever;
    private ApplicationStagingStateGetter applicationStagingStateGetter;

    public PollStageAppStatusExecution(RecentLogsRetriever recentLogsRetriever,
        ApplicationStagingStateGetter applicationStagingStateGetter) {
        super();
        this.recentLogsRetriever = recentLogsRetriever;
        this.applicationStagingStateGetter = applicationStagingStateGetter;
    }

    @Override
    public AsyncExecutionState execute(ExecutionWrapper execution) {
        execution.getStepLogger()
            .logActivitiTask();

        CloudApplication app = StepsUtil.getApp(execution.getContext());
        CloudFoundryOperations client = execution.getCloudFoundryClient();

        try {
            execution.getStepLogger()
                .debug(Messages.CHECKING_APP_STATUS, app.getName());

            Pair<ApplicationStagingState, String> state = getStagingState(execution, client, app);
            StepsUtil.saveAppLogs(execution.getContext(), client, recentLogsRetriever, app, LOGGER,
                execution.getProcessLoggerProviderFactory());
            if (!state._1.equals(ApplicationStagingState.STAGED)) {
                return checkStagingState(execution, app, state);
            }

            execution.getStepLogger()
                .info(Messages.APP_STAGED, app.getName());
            return AsyncExecutionState.FINISHED;
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            execution.getStepLogger()
                .error(e, Messages.ERROR_STAGING_APP_1, app.getName());
            throw cfe;
        } catch (SLException e) {
            execution.getStepLogger()
                .error(e, Messages.ERROR_STAGING_APP_1, app.getName());
            throw e;
        }
    }

    private Pair<ApplicationStagingState, String> getStagingState(ExecutionWrapper execution, CloudFoundryOperations client,
        CloudApplication app) {
        ApplicationStagingState applicationStagingState = applicationStagingStateGetter.getApplicationStagingState(client, app.getName());
        Pair<ApplicationStagingState, String> applicationStagingStateGuess = reportStagingLogs(execution, client, app);
        if (applicationStagingState == null) {
            return applicationStagingStateGuess;
        }
        if (applicationStagingState == ApplicationStagingState.FAILED) {
            return new Pair<>(applicationStagingState, Messages.STAGING_FAILED);
        }
        return new Pair<>(applicationStagingState, null);
    }

    private Pair<ApplicationStagingState, String> reportStagingLogs(ExecutionWrapper execution, CloudFoundryOperations client,
        CloudApplication app) {
        try {
            StartingInfo startingInfo = StepsUtil.getStartingInfo(execution.getContext());
            int offset = (Integer) execution.getContext()
                .getVariable(Constants.VAR_OFFSET);
            String stagingLogs = client.getStagingLogs(startingInfo, offset);
            if (stagingLogs != null) {
                // Staging logs successfully retrieved
                stagingLogs = stagingLogs.trim();
                if (!stagingLogs.isEmpty()) {
                    // TODO delete filtering when parallel app push is implemented
                    stagingLogs = new XMLValueFilter(stagingLogs).getFiltered();
                    execution.getStepLogger()
                        .info(stagingLogs);
                    offset += stagingLogs.length();
                    execution.getContext()
                        .setVariable(Constants.VAR_OFFSET, offset);
                }
                return new Pair<>(ApplicationStagingState.PENDING, null);
            } else {
                // No more staging logs
                return new Pair<>(ApplicationStagingState.STAGED, null);
            }
        } catch (CloudFoundryException e) {
            // "400 Bad Request" might mean that staging had already finished
            if (e.getStatusCode()
                .equals(HttpStatus.BAD_REQUEST)) {
                return new Pair<>(ApplicationStagingState.STAGED, null);
            } else {
                return new Pair<>(ApplicationStagingState.FAILED, e.getMessage());
            }
        }
    }

    private AsyncExecutionState checkStagingState(ExecutionWrapper execution, CloudApplication app,
        Pair<ApplicationStagingState, String> state) {

        if (state._1.equals(ApplicationStagingState.FAILED)) {
            // Application staging failed
            String message = format(Messages.ERROR_STAGING_APP_2, app.getName(), state._2);
            execution.getStepLogger()
                .error(message);
            return AsyncExecutionState.ERROR;
        }
        // Application not staged yet, wait and try again unless it's a timeout.
        return AsyncExecutionState.RUNNING;
    }

}
