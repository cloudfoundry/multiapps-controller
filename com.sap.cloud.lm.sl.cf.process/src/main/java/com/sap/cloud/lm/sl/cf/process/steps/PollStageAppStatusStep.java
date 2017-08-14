package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStagingState;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ApplicationStagingStateGetter;
import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.XMLValueFilter;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.Pair;

@Component("pollStageAppStatusStep")
public class PollStageAppStatusStep extends AbstractXS2ProcessStepWithBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollStageAppStatusStep.class);

    @Inject
    protected RecentLogsRetriever recentLogsRetriever;
    @Inject
    protected ApplicationStagingStateGetter applicationStagingStateGetter;

    @Override
    public String getLogicalStepName() {
        return StageAppStep.class.getSimpleName();
    }

    @Override
    protected String getIndexVariable() {
        return Constants.VAR_APPS_INDEX;
    }

    @Override
    protected ExecutionStatus pollStatusInternal(DelegateExecution context) throws Exception {
        logActivitiTask(context, LOGGER);

        CloudApplication app = StepsUtil.getApp(context);
        CloudFoundryOperations client = getCloudFoundryClient(context, LOGGER);

        try {
            debug(context, format(Messages.CHECKING_APP_STATUS, app.getName()), LOGGER);

            Pair<ApplicationStagingState, String> state = getStagingState(context, client, app);
            if (!state._1.equals(ApplicationStagingState.STAGED)) {
                return checkStagingState(context, client, app, state);
            }

            info(context, format(Messages.APP_STAGED, app.getName()), LOGGER);
            return ExecutionStatus.SUCCESS;
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            error(context, format(Messages.ERROR_STAGING_APP_1, app.getName()), e, LOGGER);
            throw e;
        } catch (SLException e) {
            error(context, format(Messages.ERROR_STAGING_APP_1, app.getName()), e, LOGGER);
            throw e;
        }
    }

    private Pair<ApplicationStagingState, String> getStagingState(DelegateExecution context, CloudFoundryOperations client,
        CloudApplication app) {
        ApplicationStagingState applicationStagingState = applicationStagingStateGetter.getApplicationStagingState(client, app.getName());
        Pair<ApplicationStagingState, String> applicationStagingStateGuess = reportStagingLogs(context, client, app);
        if (applicationStagingState == null) {
            return applicationStagingStateGuess;
        }
        if (applicationStagingState == ApplicationStagingState.FAILED) {
            return new Pair<>(applicationStagingState, Messages.STAGING_FAILED);
        }
        return new Pair<>(applicationStagingState, null);
    }

    private Pair<ApplicationStagingState, String> reportStagingLogs(DelegateExecution context, CloudFoundryOperations client,
        CloudApplication app) {
        try {
            StartingInfo startingInfo = StepsUtil.getStartingInfo(context);
            int offset = (Integer) context.getVariable(Constants.VAR_OFFSET);
            String stagingLogs = client.getStagingLogs(startingInfo, offset);
            if (stagingLogs != null) {
                // Staging logs successfully retrieved
                stagingLogs = stagingLogs.trim();
                if (!stagingLogs.isEmpty()) {
                    // TODO delete filtering when parallel app push is implemented
                    stagingLogs = new XMLValueFilter(stagingLogs).getFiltered();
                    info(context, stagingLogs, LOGGER);
                    offset += stagingLogs.length();
                    context.setVariable(Constants.VAR_OFFSET, offset);
                }
                return new Pair<>(ApplicationStagingState.PENDING, null);
            } else {
                // No more staging logs
                return new Pair<>(ApplicationStagingState.STAGED, null);
            }
        } catch (CloudFoundryException e) {
            // "400 Bad Request" might mean that staging had already finished
            if (e.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
                return new Pair<>(ApplicationStagingState.STAGED, null);
            } else {
                return new Pair<>(ApplicationStagingState.FAILED, e.getMessage());
            }
        }
    }

    private ExecutionStatus checkStagingState(DelegateExecution context, CloudFoundryOperations client, CloudApplication app,
        Pair<ApplicationStagingState, String> state) throws SLException {

        if (state._1.equals(ApplicationStagingState.FAILED)) {
            // Application staging failed
            String message = format(Messages.ERROR_STAGING_APP_2, app.getName(), state._2);
            error(context, message, LOGGER);
            StepsUtil.saveAppLogs(context, client, recentLogsRetriever, app, LOGGER, processLoggerProviderFactory);
            setRetryMessage(context, message);
            return ExecutionStatus.LOGICAL_RETRY;
        } else {
            // Application not staged yet, wait and try again unless it's a timeout
            if (StepsUtil.hasTimedOut(context, () -> System.currentTimeMillis())) {
                String message = format(Messages.APP_START_TIMED_OUT, app.getName());
                error(context, message, LOGGER);
                StepsUtil.saveAppLogs(context, client, recentLogsRetriever, app, LOGGER, processLoggerProviderFactory);
                setRetryMessage(context, message);
                return ExecutionStatus.LOGICAL_RETRY;
            }
            return ExecutionStatus.RUNNING;
        }
    }

}
