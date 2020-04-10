package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

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

import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLoggerProvider;
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
    public AsyncExecutionState execute(ProcessContext context) {
        String appToPoll = getAppToPoll(context).getName();
        CloudControllerClient client = context.getControllerClient();

        context.getStepLogger()
               .debug(Messages.CHECKING_APP_STATUS, appToPoll);

        // We're using the app object returned by the controller, because it includes the router port in its URIs, while the app model
        // we've built doesn't.
        CloudApplication app = client.getApplication(appToPoll);
        List<InstanceInfo> appInstances = getApplicationInstances(client, app);
        StartupStatus status = getStartupStatus(context, app, appInstances);
        ProcessLoggerProvider processLoggerProvider = context.getStepLogger()
                                                             .getProcessLoggerProvider();
        StepsUtil.saveAppLogs(context.getExecution(), client, recentLogsRetriever, app, LOGGER, processLoggerProvider);
        return checkStartupStatus(context, app, status);
    }

    public String getPollingErrorMessage(ProcessContext context) {
        String appToPoll = getAppToPoll(context).getName();
        return format(Messages.ERROR_STARTING_APP_0, appToPoll);
    }

    protected CloudApplication getAppToPoll(ProcessContext context) {
        return context.getVariable(Variables.APP_TO_PROCESS);
    }

    private StartupStatus getStartupStatus(ProcessContext context, CloudApplication app, List<InstanceInfo> appInstances) {
        boolean failOnCrashed = context.getVariable(Variables.FAIL_ON_CRASHED);

        if (appInstances != null) {
            int expectedInstances = app.getInstances();
            long runningInstances = getInstanceCount(appInstances, InstanceState.RUNNING);
            long flappingInstances = getInstanceCount(appInstances, InstanceState.FLAPPING);
            long crashedInstances = getInstanceCount(appInstances, InstanceState.CRASHED);
            long startingInstances = getInstanceCount(appInstances, InstanceState.STARTING);

            String states = composeStatesMessage(appInstances);

            context.getStepLogger()
                   .debug(Messages.APPLICATION_0_X_OF_Y_INSTANCES_RUNNING, app.getName(), runningInstances, expectedInstances, states);

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

    private AsyncExecutionState checkStartupStatus(ProcessContext context, CloudApplication app, StartupStatus status) {
        if (status == StartupStatus.CRASHED) {
            onError(context, Messages.ERROR_STARTING_APP_0_DESCRIPTION_1, app.getName(), Messages.SOME_INSTANCES_HAVE_CRASHED);
            return AsyncExecutionState.ERROR;
        }
        if (status == StartupStatus.FLAPPING) {
            onError(context, Messages.ERROR_STARTING_APP_0_DESCRIPTION_1, app.getName(), Messages.SOME_INSTANCES_ARE_FLAPPING);
            return AsyncExecutionState.ERROR;
        }
        if (status == StartupStatus.STARTED) {
            List<String> uris = app.getUris();
            if (uris.isEmpty()) {
                context.getStepLogger()
                       .info(Messages.APP_STARTED, app.getName());
            } else {
                context.getStepLogger()
                       .info(Messages.APP_STARTED_URLS, app.getName(), String.join(",", uris));
            }
            return AsyncExecutionState.FINISHED;
        }
        return AsyncExecutionState.RUNNING;
    }

    protected void onError(ProcessContext context, String message, Object... arguments) {
        context.getStepLogger()
               .error(message, arguments);
    }

    private String composeStatesMessage(List<InstanceInfo> instances) {
        Map<String, Long> stateCounts;
        if (instances.isEmpty()) {
            stateCounts = MapUtil.asMap(InstanceState.STARTING.toString(), 0L);
        } else {
            stateCounts = instances.stream()
                                   .collect(Collectors.groupingBy(instance -> instance.getState()
                                                                                      .toString(), Collectors.counting()));
        }
        return stateCounts.entrySet()
                          .stream()
                          .map(this::formatStateString)
                          .collect(Collectors.joining(","));
    }

    private String formatStateString(Map.Entry<String, Long> entry) {
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
