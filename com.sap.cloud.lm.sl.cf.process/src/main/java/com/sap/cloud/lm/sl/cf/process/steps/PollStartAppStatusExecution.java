package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.InstanceInfo;
import org.cloudfoundry.client.lib.domain.InstanceState;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

public class PollStartAppStatusExecution implements AsyncExecution {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollStartAppStatusExecution.class);

    enum StartupStatus {
        STARTING, STARTED, CRASHED, FLAPPING
    }

    protected RecentLogsRetriever recentLogsRetriever;
    protected ApplicationConfiguration configuration;

    public PollStartAppStatusExecution(RecentLogsRetriever recentLogsRetriever, ApplicationConfiguration configuration) {
        this.recentLogsRetriever = recentLogsRetriever;
        this.configuration = configuration;
    }

    @Override
    public AsyncExecutionState execute(ExecutionWrapper execution) {
        CloudApplication app = getAppToPoll(execution.getContext());
        CloudControllerClient client = execution.getControllerClient();

        try {
            execution.getStepLogger()
                .debug(Messages.CHECKING_APP_STATUS, app.getName());

            StartupStatus status = getStartupStatus(execution, client, app.getName());
            StepsUtil.saveAppLogs(execution.getContext(), client, recentLogsRetriever, app, LOGGER,
                execution.getProcessLoggerProviderFactory());
            return checkStartupStatus(execution, app, status);
        } catch (CloudOperationException coe) {
            CloudControllerException e = new CloudControllerException(coe);
            onError(execution, format(Messages.ERROR_STARTING_APP_1, app.getName()), e);
            throw e;
        } catch (SLException e) {
            onError(execution, format(Messages.ERROR_STARTING_APP_1, app.getName()), e);
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

    private StartupStatus getStartupStatus(ExecutionWrapper execution, CloudControllerClient client, String appName) {
        CloudApplication app = client.getApplication(appName);
        List<InstanceInfo> instances = getApplicationInstances(client, app);

        // The default value here is provided for undeploy processes:
        boolean failOnCrashed = StepsUtil.getVariableOrDefault(execution.getContext(), Constants.PARAM_FAIL_ON_CRASHED, true);

        if (instances != null) {
            int expectedInstances = app.getInstances();
            int runningInstances = getInstanceCount(instances, InstanceState.RUNNING);
            int flappingInstances = getInstanceCount(instances, InstanceState.FLAPPING);
            int crashedInstances = getInstanceCount(instances, InstanceState.CRASHED);
            int startingInstances = getInstanceCount(instances, InstanceState.STARTING);

            showInstancesStatus(execution, instances, runningInstances, expectedInstances);

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
                List<String> urls = toUrls(uris);
                execution.getStepLogger()
                    .info(Messages.APP_STARTED_URLS, app.getName(), String.join(",", urls));
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

    private void showInstancesStatus(ExecutionWrapper execution, List<InstanceInfo> instances, int runningInstances,
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
        String message = format(Messages.X_OF_Y_INSTANCES_RUNNING, runningInstances, expectedInstances, String.join(",", stateStrings));
        execution.getStepLogger()
            .info(message);
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

    private List<String> toUrls(List<String> uris) {
        String protocolPrefix = getProtocolPrefix();
        return uris.stream()
            .map(uri -> protocolPrefix + uri)
            .collect(Collectors.toList());
    }

    private String getProtocolPrefix() {
        return configuration.getTargetURL()
            .getProtocol() + "://";
    }

}
