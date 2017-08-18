package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
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
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PollStageAppStatusStep extends AbstractXS2ProcessStepWithBridge {

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
        getStepLogger().logActivitiTask();

        CloudApplication app = StepsUtil.getApp(context);
        CloudFoundryOperations client = getCloudFoundryClient(context);

        try {
            getStepLogger().debug(Messages.CHECKING_APP_STATUS, app.getName());

            Pair<ApplicationStagingState, String> state = getStagingState(context, client, app);
            if (!state._1.equals(ApplicationStagingState.STAGED)) {
                return checkStagingState(context, client, app, state);
            }

            getStepLogger().info(Messages.APP_STAGED, app.getName());
            return ExecutionStatus.SUCCESS;
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            getStepLogger().error(e, Messages.ERROR_STAGING_APP_1, app.getName());
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_STAGING_APP_1, app.getName());
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
                    getStepLogger().info(stagingLogs);
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
            getStepLogger().error(message);
            StepsUtil.saveAppLogs(context, client, recentLogsRetriever, app, logger, processLoggerProviderFactory);
            setRetryMessage(context, message);
            return ExecutionStatus.LOGICAL_RETRY;
        } else {
            // Application not staged yet, wait and try again unless it's a timeout
            if (StepsUtil.hasTimedOut(context, () -> System.currentTimeMillis())) {
                String message = format(Messages.APP_START_TIMED_OUT, app.getName());
                getStepLogger().error(message);
                StepsUtil.saveAppLogs(context, client, recentLogsRetriever, app, logger, processLoggerProviderFactory);
                setRetryMessage(context, message);
                return ExecutionStatus.LOGICAL_RETRY;
            }
            return ExecutionStatus.RUNNING;
        }
    }

}
