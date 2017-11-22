package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.ApplicationLog.MessageType;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationAttributesGetter;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.Pair;

@Component("pollExecuteAppStatusStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PollExecuteAppStatusStep extends AbstractProcessStepWithBridge {

    enum AppExecutionStatus {
        EXECUTING, SUCCEEDED, FAILED
    }

    private static final String DEFAULT_SUCCESS_MARKER = "STDOUT:SUCCESS";
    private static final String DEFAULT_FAILURE_MARKER = "STDERR:FAILURE";

    @Inject
    protected RecentLogsRetriever recentLogsRetriever;

    @Override
    public String getLogicalStepName() {
        return StartAppStep.class.getSimpleName();
    }

    @Override
    public String getIndexVariable() {
        return Constants.VAR_APPS_INDEX;
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws Exception {
        getStepLogger().logActivitiTask();
        CloudApplication app = getNextApp(context);
        ApplicationAttributesGetter attributesGetter = ApplicationAttributesGetter.forApplication(app);
        boolean executeApp = attributesGetter.getAttribute(SupportedParameters.EXECUTE_APP, Boolean.class, false);

        if (!executeApp) {
            return ExecutionStatus.SUCCESS;
        }
        try {
            CloudFoundryOperations client = getCloudFoundryClient(context);
            Pair<AppExecutionStatus, String> status = getAppExecutionStatus(context, client, attributesGetter, app);
            return checkAppExecutionStatus(context, client, attributesGetter, app, status);
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            getStepLogger().error(e, Messages.ERROR_EXECUTING_APP_1, app.getName());
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_EXECUTING_APP_1, app.getName());
            throw e;
        }
    }

    protected CloudApplication getNextApp(DelegateExecution context) {
        return StepsUtil.getApp(context);
    }

    private Pair<AppExecutionStatus, String> getAppExecutionStatus(DelegateExecution context, CloudFoundryOperations client,
        ApplicationAttributesGetter attributesGetter, CloudApplication app) throws SLException {
        Pair<AppExecutionStatus, String> status = new Pair<>(AppExecutionStatus.EXECUTING, null);
        long startTime = (Long) context.getVariable(Constants.VAR_START_TIME);
        Pair<MessageType, String> sm = getMarker(attributesGetter, SupportedParameters.SUCCESS_MARKER, DEFAULT_SUCCESS_MARKER);
        Pair<MessageType, String> fm = getMarker(attributesGetter, SupportedParameters.FAILURE_MARKER, DEFAULT_FAILURE_MARKER);
        boolean checkDeployId = attributesGetter.getAttribute(SupportedParameters.CHECK_DEPLOY_ID, Boolean.class, false);
        String deployId = checkDeployId ? (StepsUtil.DEPLOY_ID_PREFIX + StepsUtil.getCorrelationId(context)) : null;

        List<ApplicationLog> recentLogs = recentLogsRetriever.getRecentLogs(client, app.getName());
        if (recentLogs != null) {
            Optional<Pair<AppExecutionStatus, String>> statusx = recentLogs.stream().map(
                log -> getAppExecutionStatus(log, startTime, sm, fm, deployId)).filter(aes -> (aes != null)).reduce((a, b) -> b);
            if (statusx.isPresent()) {
                status = statusx.get();
            }
        }
        return status;
    }

    private Pair<AppExecutionStatus, String> getAppExecutionStatus(ApplicationLog log, long startTime, Pair<MessageType, String> sm,
        Pair<MessageType, String> fm, String id) {
        long time = log.getTimestamp().getTime();
        String sourceName = log.getSourceName();
        sourceName = (sourceName.length() >= 3) ? sourceName.substring(0, 3) : sourceName;
        if (time < startTime || !sourceName.equalsIgnoreCase("APP"))
            return null;
        MessageType mt = log.getMessageType();
        String msg = log.getMessage().trim();
        if (mt != null && mt.equals(sm._1) && msg.matches(sm._2) && ((id == null) || msg.contains(id))) {
            return new Pair<>(AppExecutionStatus.SUCCEEDED, null);
        } else if (mt != null && mt.equals(fm._1) && msg.matches(fm._2) && ((id == null) || msg.contains(id))) {
            return new Pair<>(AppExecutionStatus.FAILED, msg);
        } else
            return null;
    }

    private ExecutionStatus checkAppExecutionStatus(DelegateExecution context, CloudFoundryOperations client,
        ApplicationAttributesGetter attributesGetter, CloudApplication app, Pair<AppExecutionStatus, String> status) throws SLException {
        if (status._1.equals(AppExecutionStatus.FAILED)) {
            // Application execution failed
            String message = format(Messages.ERROR_EXECUTING_APP_2, app.getName(), status._2);
            getStepLogger().error(message);
            StepsUtil.saveAppLogs(context, client, recentLogsRetriever, app, LOGGER.getLoggerImpl(), processLoggerProviderFactory);
            setRetryMessage(context, message);
            return ExecutionStatus.LOGICAL_RETRY;
        } else if (status._1.equals(AppExecutionStatus.SUCCEEDED)) {
            // Application executed successfully
            getStepLogger().info(Messages.APP_EXECUTED, app.getName());
            StepsUtil.saveAppLogs(context, client, recentLogsRetriever, app, LOGGER.getLoggerImpl(), processLoggerProviderFactory);
            // Stop the application if specified
            boolean stopApp = attributesGetter.getAttribute(SupportedParameters.STOP_APP, Boolean.class, false);
            if (stopApp) {
                getStepLogger().info(Messages.STOPPING_APP, app.getName());
                client.stopApplication(app.getName());
                getStepLogger().debug(Messages.APP_STOPPED, app.getName());
            }
            return ExecutionStatus.SUCCESS;
        } else {
            // Application not executed yet, wait and try again unless it's a timeout
            if (StepsUtil.hasTimedOut(context, () -> System.currentTimeMillis())) {
                String message = format(Messages.APP_START_TIMED_OUT, app.getName());
                getStepLogger().error(message);
                StepsUtil.saveAppLogs(context, client, recentLogsRetriever, app, LOGGER.getLoggerImpl(), processLoggerProviderFactory);
                setRetryMessage(context, message);
                return ExecutionStatus.LOGICAL_RETRY;
            }
            return ExecutionStatus.RUNNING;
        }
    }

    private static Pair<MessageType, String> getMarker(ApplicationAttributesGetter attributesGetter, String attribute, String defaultValue)
        throws SLException {
        MessageType messageType;
        String text;
        String attr = attributesGetter.getAttribute(attribute, String.class, defaultValue);
        if (attr.startsWith(MessageType.STDERR.toString() + ":")) {
            messageType = MessageType.STDERR;
            text = attr.substring(MessageType.STDERR.toString().length() + 1);
        } else if (attr.startsWith(MessageType.STDOUT.toString() + ":")) {
            messageType = MessageType.STDOUT;
            text = attr.substring(MessageType.STDOUT.toString().length() + 1);
        } else {
            messageType = MessageType.STDOUT;
            text = attr;
        }
        return new Pair<MessageType, String>(messageType, text);
    }

}
