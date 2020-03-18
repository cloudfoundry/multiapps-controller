package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.InstanceInfo;
import org.cloudfoundry.client.lib.domain.InstanceState;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLoggerProvider;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

public class PollStartAppStatusExecution implements AsyncExecution {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollStartAppStatusExecution.class);

    enum StartupStatus {
        STARTING, STARTED, CRASHED, FLAPPING
    }

    private final RecentLogsRetriever recentLogsRetriever;

    public PollStartAppStatusExecution(RecentLogsRetriever recentLogsRetriever) {
        this.recentLogsRetriever = recentLogsRetriever;
    }

    @Override
    public AsyncExecutionState execute(ExecutionWrapper execution) {
        String appToPoll = getAppToPoll(execution).getName();
        CloudControllerClient client = execution.getControllerClient();

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
    }

    public String getPollingErrorMessage(ExecutionWrapper execution) {
        String appToPoll = getAppToPoll(execution).getName();
        return format(Messages.ERROR_STARTING_APP_0, appToPoll);
    }

    protected CloudApplication getAppToPoll(ExecutionWrapper execution) {
        return execution.getVariable(Variables.APP_TO_PROCESS);
    }

    private StartupStatus getStartupStatus(ExecutionWrapper execution, CloudApplication app, List<InstanceInfo> appInstances) {
        // The default value here is provided for undeploy processes:
        boolean failOnCrashed = StepsUtil.getBoolean(execution.getContext(), Constants.PARAM_FAIL_ON_CRASHED, true);

        if (appInstances != null) {
            int expectedInstances = app.getInstances();
            long runningInstances = getInstanceCount(appInstances, InstanceState.RUNNING);
            long flappingInstances = getInstanceCount(appInstances, InstanceState.FLAPPING);
            long crashedInstances = getInstanceCount(appInstances, InstanceState.CRASHED);
            long startingInstances = getInstanceCount(appInstances, InstanceState.STARTING);

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
        if (status == StartupStatus.CRASHED) {
            onError(execution, Messages.ERROR_STARTING_APP_0_DESCRIPTION_1, app.getName(), Messages.SOME_INSTANCES_HAVE_CRASHED);
            return AsyncExecutionState.ERROR;
        }
        if (status == StartupStatus.FLAPPING) {
            onError(execution, Messages.ERROR_STARTING_APP_0_DESCRIPTION_1, app.getName(), Messages.SOME_INSTANCES_ARE_FLAPPING);
            return AsyncExecutionState.ERROR;
        }
        if (status == StartupStatus.STARTED) {
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

    protected void onError(ExecutionWrapper execution, String message, Object... arguments) {
        execution.getStepLogger()
                 .error(message, arguments);
    }

    private void showInstancesStatus(ExecutionWrapper execution, String appName, List<InstanceInfo> instances, long runningInstances,
                                     int expectedInstances) {

        // Determine state counts
        Map<String, Integer> stateCounts = new HashMap<>();
        if (instances.isEmpty()) {
            stateCounts.put(InstanceState.STARTING.toString(), 0);
        } else {
            for (InstanceInfo instance : instances) {
                String state = instance.getState()
                                       .toString();
                incrementStateCount(stateCounts, state);
            }
        }

        // Compose state strings
        String states = stateCounts.entrySet()
                                   .stream()
                                   .map(this::formatStateString)
                                   .collect(Collectors.joining(","));

        // Print message
        String message = format(Messages.APPLICATION_0_X_OF_Y_INSTANCES_RUNNING, appName, runningInstances, expectedInstances, states);
        execution.getStepLogger()
                 .debug(message);
    }

    private void incrementStateCount(Map<String, Integer> stateCounts, String state) {
        int stateCount = stateCounts.getOrDefault(state, 1);
        stateCounts.put(state, stateCount + 1);
    }

    private String formatStateString(Map.Entry<String, Integer> entry) {
        return format("{0} {1}", entry.getValue(), entry.getKey()
                                                        .toLowerCase());
    }

    private List<InstanceInfo> getApplicationInstances(CloudControllerClient client, CloudApplication app) {
        InstancesInfo instancesInfo = client.getApplicationInstances(app);
        return (instancesInfo != null) ? instancesInfo.getInstances() : null;
    }

    private long getInstanceCount(List<InstanceInfo> instances, InstanceState state) {
        return instances.stream()
                        .filter(instance -> instance.getState()
                                                    .equals(state))
                        .count();
    }
}
