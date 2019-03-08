package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.InstanceInfo;
import org.cloudfoundry.client.lib.domain.InstanceState;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLoggerProvider;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

public class PollStartAppStatusExecution implements AsyncExecution {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollStartAppStatusExecution.class);

    enum StartupStatus {
        STARTING, STARTED, CRASHED, FLAPPING
    }

    private RecentLogsRetriever recentLogsRetriever;

    public PollStartAppStatusExecution(RecentLogsRetriever recentLogsRetriever) {
        this.recentLogsRetriever = recentLogsRetriever;
    }

    @Override
    public AsyncExecutionState execute(ExecutionWrapper execution) {
        String appToPoll = getAppToPoll(execution.getContext()).getName();
        CloudControllerClient client = execution.getControllerClient();

        try {
            execution.getStepLogger()
                .debug(Messages.CHECKING_APP_STATUS, appToPoll);

            // We're using the app object returned by the controller, because it includes the router port in its URIs, while the app model
            // we've built doesn't.
            CloudApplication app = client.getApplication(appToPoll);
            List<InstanceInfo> appInstances = getApplicationInstances(client, app);
            StartupStatus status = getStartupStatus(execution, app, appInstances);
            ProcessLoggerProvider processLoggerProvider = execution.getStepLogger()
                .getProcessLoggerProvider();
            StepsUtil.saveAppLogs(execution.getContext(), client, recentLogsRetriever, app, LOGGER, processLoggerProvider);
            return checkStartupStatus(execution, app, status);
        } catch (CloudOperationException coe) {
            CloudControllerException e = new CloudControllerException(coe);
            onError(execution, format(Messages.ERROR_STARTING_APP_1, appToPoll), e);
            throw e;
        } catch (SLException e) {
            onError(execution, format(Messages.ERROR_STARTING_APP_1, appToPoll), e);
            throw e;
        }
    }

    protected void onError(ExecutionWrapper execution, String message, Exception e) {
        execution.getStepLogger()
            .error(e, message);
    }

    protected void onError(ExecutionWrapper execution, String message) {
        execution.getStepLogger()
            .error(message);
    }

    protected CloudApplication getAppToPoll(DelegateExecution context) {
        return StepsUtil.getApp(context);
    }

    private StartupStatus getStartupStatus(ExecutionWrapper execution, CloudApplication app, List<InstanceInfo> appInstances) {
        // The default value here is provided for undeploy processes:
        boolean failOnCrashed = StepsUtil.getVariableOrDefault(execution.getContext(), Constants.PARAM_FAIL_ON_CRASHED, true);

        if (appInstances != null) {
            int expectedInstances = app.getInstances();
            int runningInstances = getInstanceCount(appInstances, InstanceState.RUNNING);
            int flappingInstances = getInstanceCount(appInstances, InstanceState.FLAPPING);
            int crashedInstances = getInstanceCount(appInstances, InstanceState.CRASHED);
            int startingInstances = getInstanceCount(appInstances, InstanceState.STARTING);

            showInstancesStatus(execution, app.getName(), appInstances, runningInstances, expectedInstances);

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

    private AsyncExecutionState checkStartupStatus(ExecutionWrapper execution, CloudApplication app, StartupStatus status) {
        if (status.equals(StartupStatus.CRASHED) || status.equals(StartupStatus.FLAPPING)) {
            // Application failed to start
            String message = format(Messages.ERROR_STARTING_APP_2, app.getName(), getMessageForStatus(status));
            onError(execution, message);
            return AsyncExecutionState.ERROR;
        } else if (status.equals(StartupStatus.STARTED)) {
            // Application started successfully
            List<String> uris = app.getUris();
            if (uris.isEmpty()) {
                execution.getStepLogger()
                    .info(Messages.APP_STARTED, app.getName());
            } else {
                execution.getStepLogger()
                    .info(Messages.APP_STARTED_URLS, app.getName(), String.join(",", uris));
            }
            return AsyncExecutionState.FINISHED;
        }
        return AsyncExecutionState.RUNNING;
    }

    protected String getMessageForStatus(StartupStatus status) {
        if (status.equals(StartupStatus.FLAPPING)) {
            return Messages.SOME_INSTANCES_ARE_FLAPPING;
        } else if (status.equals(StartupStatus.CRASHED)) {
            return Messages.SOME_INSTANCES_HAVE_CRASHED;
        }
        return null;
    }

    private void showInstancesStatus(ExecutionWrapper execution, String appName, List<InstanceInfo> instances, int runningInstances,
        int expectedInstances) {

        // Determine state counts
        Map<String, Integer> stateCounts = new HashMap<>();
        if (instances.isEmpty()) {
            stateCounts.put(InstanceState.STARTING.toString(), 0);
        } else {
            for (InstanceInfo instance : instances) {
                final String state = instance.getState()
                    .toString();
                final Integer stateCount = stateCounts.get(state);
                stateCounts.put(state, (stateCount == null) ? 1 : (stateCount + 1));
            }
        }

        // Compose state strings
        List<String> stateStrings = new ArrayList<>();
        for (Map.Entry<String, Integer> sc : stateCounts.entrySet()) {
            stateStrings.add(format("{0} {1}", sc.getValue(), sc.getKey()
                .toLowerCase()));
        }

        // Print message
        String message = format(Messages.APPLICATION_0_X_OF_Y_INSTANCES_RUNNING, appName, runningInstances, expectedInstances,
            String.join(",", stateStrings));
        execution.getStepLogger()
            .debug(message);
    }

    private static List<InstanceInfo> getApplicationInstances(CloudControllerClient client, CloudApplication app) {
        InstancesInfo instancesInfo = client.getApplicationInstances(app);
        return (instancesInfo != null) ? instancesInfo.getInstances() : null;
    }

    private static int getInstanceCount(List<InstanceInfo> instances, InstanceState state) {
        int count = 0;
        for (InstanceInfo instance : instances) {
            if (instance.getState()
                .equals(state)) {
                count++;
            }
        }
        return count;
    }

}
