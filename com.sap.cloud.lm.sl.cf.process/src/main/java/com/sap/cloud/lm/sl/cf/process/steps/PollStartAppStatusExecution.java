package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.InstanceInfo;
import org.cloudfoundry.client.lib.domain.InstanceState;
import org.cloudfoundry.client.lib.domain.InstancesInfo;

import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.CommonUtil;

public class PollStartAppStatusExecution extends AsyncExecution {

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
    public AsyncExecutionState execute(ExecutionWrapper execution) throws SLException {
        execution.getStepLogger().logActivitiTask();

        CloudApplication app = getAppToPoll(execution.getContext());
        CloudFoundryOperations client = execution.getCloudFoundryClient();

        try {
            execution.getStepLogger().debug(Messages.CHECKING_APP_STATUS, app.getName());

            StartupStatus status = getStartupStatus(execution, client, app.getName());
            return checkStartupStatus(execution, client, app, status);
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            onError(execution, format(Messages.ERROR_STARTING_APP_1, app.getName()), e);
            throw cfe;
        } catch (SLException e) {
            onError(execution, format(Messages.ERROR_STARTING_APP_1, app.getName()), e);
            throw e;
        }
    }

    protected void onError(ExecutionWrapper execution, String message, Exception e) {
        execution.getStepLogger().error(e, message);
    }

    protected void onError(ExecutionWrapper execution, String message) {
        execution.getStepLogger().error(message);
    }

    protected CloudApplication getAppToPoll(DelegateExecution context) {
        return StepsUtil.getApp(context);
    }

    private StartupStatus getStartupStatus(ExecutionWrapper execution, CloudFoundryOperations client, String appName) {
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

    private AsyncExecutionState checkStartupStatus(ExecutionWrapper execution, CloudFoundryOperations client, CloudApplication app,
        StartupStatus status) throws SLException {

        StepsUtil.saveAppLogs(execution.getContext(), client, recentLogsRetriever, app, LOGGER,
            execution.getProcessLoggerProviderFactory());
        if (status.equals(StartupStatus.CRASHED) || status.equals(StartupStatus.FLAPPING)) {
            // Application failed to start
            String message = format(Messages.ERROR_STARTING_APP_2, app.getName(), getMessageForStatus(status));
            onError(execution, message);
            return AsyncExecutionState.ERROR;
        } else if (status.equals(StartupStatus.STARTED)) {
            // Application started successfully
            List<String> uris = app.getUris();
            if (uris.isEmpty()) {
                execution.getStepLogger().info(Messages.APP_STARTED, app.getName());
            } else {
                String urls = CommonUtil.toCommaDelimitedString(uris, getProtocolPrefix());
                execution.getStepLogger().info(Messages.APP_STARTED_URLS, app.getName(), urls);
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
        execution.getStepLogger().info(message);
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
        return configuration.getTargetURL().getProtocol() + "://";
    }

}
