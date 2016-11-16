package com.sap.cloud.lm.sl.cf.process.steps;

import static com.sap.cloud.lm.sl.cf.process.steps.StepsUtil.appLog;
import static java.text.MessageFormat.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.StreamingLogToken;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.ApplicationLog.MessageType;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.InstanceInfo;
import org.cloudfoundry.client.lib.domain.InstanceState;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.activiti.common.util.ContextUtil;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.core.helpers.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.XMLValueFilter;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.CommonUtil;
import com.sap.cloud.lm.sl.common.util.Pair;

@Component("pollStartAppStatusStep")
public class PollStartAppStatusStep extends AbstractXS2ProcessStepWithBridge {

    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(PollStartAppStatusStep.class);

    private static final String DEFAULT_SUCCESS_MARKER = "STDOUT:SUCCESS";
    private static final String DEFAULT_FAILURE_MARKER = "STDERR:FAILURE";

    private static final Integer DEFAULT_TIMEOUT = 900;

    enum StagingStatus {
        STAGING, FINISHED, FAILED
    }

    enum StartupStatus {
        STARTING, STARTED, CRASHED, FLAPPING
    }

    enum AppExecutionStatus {
        EXECUTING, SUCCEEDED, FAILED
    }

    @Autowired
    protected RecentLogsRetriever recentLogsRetriever;

    @Override
    protected ExecutionStatus pollStatusInternal(DelegateExecution context) throws SLException {

        logActivitiTask(context, LOGGER);

        // Get the next cloud application from the context
        CloudApplication app = StepsUtil.getApp(context);

        try {
            debug(context, format(Messages.CHECKING_APP_STATUS, app.getName()), LOGGER);

            // Get a cloud foundry client
            CloudFoundryOperations client = getCloudFoundryClient(context, LOGGER);

            // Check the start phase
            while (true) {
                String startPhase = (String) context.getVariable(Constants.VAR_START_PHASE);
                debug(context, format(Messages.CURRENT_START_PHASE, startPhase), LOGGER);
                if (startPhase.equals(Constants.START_PHASE_STAGING)) {
                    Pair<StagingStatus, String> status = getStagingStatusEx(context, app, client);
                    if (status._1.equals(StagingStatus.FINISHED)) {
                        info(context, format(Messages.APP_STAGED, app.getName()), LOGGER);
                        context.setVariable(Constants.VAR_START_PHASE, Constants.START_PHASE_STARTUP);
                    } else {
                        return checkStagingStatus(context, app, client, status);
                    }
                } else if (startPhase.equals(Constants.START_PHASE_STARTUP)) {
                    StartupStatus status = getStartupStatus(context, app.getName(), client);
                    boolean executeApp = StepsUtil.getAppAttribute(app, SupportedParameters.EXECUTE_APP, false);
                    if (status.equals(StartupStatus.STARTED) && executeApp) {
                        info(context, format(Messages.APP_STARTED, app.getName()), LOGGER);
                        context.setVariable(Constants.VAR_START_PHASE, Constants.START_PHASE_EXECUTION);
                    } else {
                        return checkStartupStatus(context, app, client, status);
                    }
                } else if (startPhase.equals(Constants.START_PHASE_EXECUTION)) {
                    Pair<AppExecutionStatus, String> status = getAppExecutionStatus(context, app, client);
                    return checkAppExecutionStatus(context, app, client, status);
                }
            }
        } catch (SLException e) {
            error(context, format(Messages.ERROR_STARTING_APP, app.getName()), e, LOGGER);
            throw e;
        } catch (CloudFoundryException e) {
            SLException ex = StepsUtil.createException(e);
            error(context, format(Messages.ERROR_STARTING_APP, app.getName()), ex, LOGGER);
            throw ex;
        }
    }

    private Pair<StagingStatus, String> getStagingStatusEx(DelegateExecution context, CloudApplication app, CloudFoundryOperations client)
        throws SLException {

        Pair<StagingStatus, String> status = getStagingStatus(context, app, client);

        if (status._1.equals(StagingStatus.FINISHED)) {
            // Staging has finished successfully
            // If client supports extensions (XS2), start the application without staging
            ClientExtensions clientExtensions = getClientExtensions(context, LOGGER);
            if (clientExtensions != null) {
                clientExtensions.startApplication(app.getName(), false);
            }
        }
        return status;
    }

    private Pair<StagingStatus, String> getStagingStatus(DelegateExecution context, CloudApplication app, CloudFoundryOperations client) {
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
                return new Pair<>(StagingStatus.STAGING, null);
            } else {
                // No more staging logs
                return new Pair<>(StagingStatus.FINISHED, null);
            }
        } catch (CloudFoundryException e) {
            // "400 Bad Request" might mean that staging had already finished
            if (e.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
                return new Pair<>(StagingStatus.FINISHED, null);
            } else {
                return new Pair<>(StagingStatus.FAILED, e.getMessage());
            }
        }
    }

    private ExecutionStatus checkStagingStatus(DelegateExecution context, CloudApplication app, CloudFoundryOperations client,
        Pair<StagingStatus, String> status) throws SLException {

        if (status._1.equals(StagingStatus.FAILED)) {
            // Application staging failed
            String message = format(Messages.ERROR_STAGING_APP, app.getName(), status._2);
            error(context, message, LOGGER);
            finalizeAppLogs(context, app, client);
            setRetryMessage(context, message);
            return ExecutionStatus.LOGICAL_RETRY;
        } else {
            // Application not staged yet, wait and try again unless it's a timeout
            getBridge().incrementCounter(context);
            return checkTimeout(context, app, client);
        }
    }

    private StartupStatus getStartupStatus(DelegateExecution context, String appName, CloudFoundryOperations client) {
        CloudApplication app = client.getApplication(appName);
        List<InstanceInfo> instances = getApplicationInstances(client, app);

        boolean failOnCrashed = ContextUtil.getVariable(context, Constants.PARAM_FAIL_ON_CRASHED, true);

        if (instances != null) {
            int expectedInstances = app.getInstances();
            int runningInstances = getInstanceCount(instances, InstanceState.RUNNING);
            int flappingInstances = getInstanceCount(instances, InstanceState.FLAPPING);
            int crashedInstances = getInstanceCount(instances, InstanceState.CRASHED);
            int startingInstances = getInstanceCount(instances, InstanceState.STARTING);

            showInstancesStatus(context, instances, runningInstances, expectedInstances);

            if (runningInstances == expectedInstances) {
                return StartupStatus.STARTED;
            }
            if (startingInstances > 0) {
                return StartupStatus.STARTING;
            }
            if (flappingInstances > 0) {
                return StartupStatus.FLAPPING;
            }
            if (crashedInstances > 0 && failOnCrashed) {
                return StartupStatus.CRASHED;
            }
        }

        return StartupStatus.STARTING;
    }

    private ExecutionStatus checkStartupStatus(DelegateExecution context, CloudApplication app, CloudFoundryOperations client,
        StartupStatus status) throws SLException {

        if (status.equals(StartupStatus.CRASHED) || status.equals(StartupStatus.FLAPPING)) {
            // Application failed to start
            String message = format(Messages.ERROR_STARTING_APP2, app.getName(), getMessageForStatus(status));
            error(context, message, LOGGER);
            finalizeAppLogs(context, app, client);
            setRetryMessage(context, message);
            return ExecutionStatus.LOGICAL_RETRY;
        } else if (status.equals(StartupStatus.STARTED)) {
            // Application started successfully
            List<String> uris = app.getUris();
            if (uris.isEmpty()) {
                info(context, format(Messages.APP_STARTED, app.getName()), LOGGER);
            } else {
                String urls = CommonUtil.toCommaDelimitedString(uris, getProtocolPrefix());
                info(context, format(Messages.APP_STARTED_URLS, app.getName(), urls), LOGGER);
            }
            finalizeAppLogs(context, app, client);
            return ExecutionStatus.SUCCESS;
        } else {
            // Application not started yet, wait and try again unless it's a timeout
            getBridge().incrementCounter(context);
            return checkTimeout(context, app, client);
        }
    }

    private String getMessageForStatus(StartupStatus status) {
        if (status.equals(StartupStatus.FLAPPING)) {
            return "Some instances are flapping";
        } else if (status.equals(StartupStatus.CRASHED)) {
            return "Some instances have crashed";
        } else {
            return null;
        }
    }

    private Pair<AppExecutionStatus, String> getAppExecutionStatus(DelegateExecution context, CloudApplication app,
        CloudFoundryOperations client) throws SLException {
        Pair<AppExecutionStatus, String> status = new Pair<>(AppExecutionStatus.EXECUTING, null);
        long startTime = (Long) context.getVariable(Constants.VAR_START_TIME);
        Pair<MessageType, String> sm = getMarker(app, SupportedParameters.SUCCESS_MARKER, DEFAULT_SUCCESS_MARKER);
        Pair<MessageType, String> fm = getMarker(app, SupportedParameters.FAILURE_MARKER, DEFAULT_FAILURE_MARKER);
        boolean checkDeployId = StepsUtil.getAppAttribute(app, SupportedParameters.CHECK_DEPLOY_ID, false);
        String deployId = checkDeployId ? (BuildCloudDeployModelStep.DEPLOY_ID_PREFIX + context.getId()) : null;

        List<ApplicationLog> recentLogs = recentLogsRetriever.getRecentLogs(client, app.getName());
        if (recentLogs != null) {
            // @formatter:off

            Optional<Pair<AppExecutionStatus, String>> statusx = recentLogs.stream()
                .map(log -> getAppExecutionStatus(log, startTime, sm, fm, deployId))
                .filter(aes -> (aes != null))
                .reduce((a, b) -> b);

            // @formatter:on
            if (statusx.isPresent()) {
                status = statusx.get();
            }
        }
        return status;
    }

    private static Pair<MessageType, String> getMarker(CloudApplication app, String attribute, String defaultValue) throws SLException {
        MessageType messageType;
        String text;
        String attr = StepsUtil.getAppAttribute(app, attribute, defaultValue);
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

    private ExecutionStatus checkAppExecutionStatus(DelegateExecution context, CloudApplication app, CloudFoundryOperations client,
        Pair<AppExecutionStatus, String> status) throws SLException {
        if (status._1.equals(AppExecutionStatus.FAILED)) {
            // Application execution failed
            String message = format(Messages.ERROR_EXECUTING_APP, app.getName(), status._2);
            error(context, message, LOGGER);
            finalizeAppLogs(context, app, client);
            setRetryMessage(context, message);
            return ExecutionStatus.LOGICAL_RETRY;
        } else if (status._1.equals(AppExecutionStatus.SUCCEEDED)) {
            // Application executed successfully
            info(context, format(Messages.APP_EXECUTED, app.getName()), LOGGER);
            finalizeAppLogs(context, app, client);
            // Stop the application if specified
            boolean stopApp = StepsUtil.getAppAttribute(app, SupportedParameters.STOP_APP, false);
            if (stopApp) {
                info(context, format(Messages.STOPPING_APP, app.getName()), LOGGER);
                client.stopApplication(app.getName());
                debug(context, format(Messages.APP_STOPPED, app.getName()), LOGGER);
            }
            return ExecutionStatus.SUCCESS;
        } else {
            // Application not executed yet, wait and try again unless it's a timeout
            getBridge().incrementCounter(context);
            return checkTimeout(context, app, client);
        }
    }

    private ExecutionStatus checkTimeout(DelegateExecution context, CloudApplication app, CloudFoundryOperations client)
        throws SLException {
        int timeout = ContextUtil.getVariable(context, Constants.PARAM_START_TIMEOUT, DEFAULT_TIMEOUT);
        long startTime = (Long) context.getVariable(Constants.VAR_START_TIME);
        long currentTime = System.currentTimeMillis();
        if ((currentTime - startTime) > timeout * 1000) {
            String message = format(Messages.APP_START_TIMED_OUT, app.getName());
            error(context, message, LOGGER);
            finalizeAppLogs(context, app, client);
            setRetryMessage(context, message);
            return ExecutionStatus.LOGICAL_RETRY;
        } else {
            return ExecutionStatus.RUNNING;
        }
    }

    private void finalizeAppLogs(DelegateExecution context, CloudApplication app, CloudFoundryOperations client) {
        boolean streamAppLogs = ContextUtil.getVariable(context, Constants.PARAM_STREAM_APP_LOGS, false);
        if (streamAppLogs) {
            StreamingLogToken streamingLogsToken = StepsUtil.getStreamingLogsToken(context);
            if (streamingLogsToken != null) {
                streamingLogsToken.cancel();
            }
        } else {
            List<ApplicationLog> recentLogs = recentLogsRetriever.getRecentLogs(client, app.getName());
            if (recentLogs != null) {
                recentLogs.forEach(log -> appLog(context, app.getName(), log.toString(), LOGGER, processLoggerProviderFactory));
            }
        }
    }

    @Override
    public String getLogicalStepName() {
        return StartAppStep.class.getSimpleName();
    }

    private void showInstancesStatus(DelegateExecution context, List<InstanceInfo> instances, int runningInstances, int expectedInstances) {

        // Determine state counts
        Map<String, Integer> stateCounts = new HashMap<>();
        if (instances.isEmpty()) {
            stateCounts.put(InstanceState.STARTING.toString(), 0);
        } else {
            for (InstanceInfo instance : instances) {
                final String state = instance.getState().toString();
                final Integer stateCount = stateCounts.get(state);
                stateCounts.put(state, (stateCount == null) ? 1 : (stateCount + 1));
            }
        }

        // Compose state strings
        List<String> stateStrings = new ArrayList<>();
        for (Map.Entry<String, Integer> sc : stateCounts.entrySet()) {
            stateStrings.add(format("{0} {1}", sc.getValue(), sc.getKey().toLowerCase()));
        }

        // Print message
        String message = format(Messages.X_OF_Y_INSTANCES_RUNNING, runningInstances, expectedInstances,
            CommonUtil.toCommaDelimitedString(stateStrings, ""));
        info(context, message, LOGGER);
    }

    private static List<InstanceInfo> getApplicationInstances(CloudFoundryOperations client, CloudApplication app) {
        InstancesInfo instancesInfo = client.getApplicationInstances(app);
        return (instancesInfo != null) ? instancesInfo.getInstances() : null;
    }

    private static int getInstanceCount(List<InstanceInfo> instances, InstanceState state) {
        int count = 0;
        for (InstanceInfo instance : instances) {
            if (instance.getState().equals(state)) {
                count++;
            }
        }
        return count;
    }

    private String getProtocolPrefix() {
        return ConfigurationUtil.getTargetURL().getProtocol() + "://";
    }

}
