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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.core.util.Configuration;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.CommonUtil;

@Component("pollStartAppStatusStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PollStartAppStatusStep extends AbstractProcessStep {

    @Override
    public String getLogicalStepName() {
        return StartAppStep.class.getSimpleName();
    }

    enum StartupStatus {
        STARTING, STARTED, CRASHED, FLAPPING
    }

    @Autowired
    protected RecentLogsRetriever recentLogsRetriever;
    @Autowired
    protected Configuration configuration;

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        getStepLogger().logActivitiTask();

        CloudApplication app = getAppToPoll(context);
        CloudFoundryOperations client = getCloudFoundryClient(context);

        try {
            getStepLogger().debug(Messages.CHECKING_APP_STATUS, app.getName());

            StartupStatus status = getStartupStatus(context, client, app.getName());
            return checkStartupStatus(context, client, app, status);
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            onError(format(Messages.ERROR_STARTING_APP_1, app.getName()), e);
            throw e;
        } catch (SLException e) {
            onError(format(Messages.ERROR_STARTING_APP_1, app.getName()), e);
            throw e;
        }
    }

    protected void onError(String message, Exception e) {
        getStepLogger().error(e, message);
    }

    protected void onError(String message) {
        getStepLogger().error(message);
    }

    protected CloudApplication getAppToPoll(DelegateExecution context) {
        return StepsUtil.getApp(context);
    }

    private StartupStatus getStartupStatus(DelegateExecution context, CloudFoundryOperations client, String appName) {
        CloudApplication app = client.getApplication(appName);
        List<InstanceInfo> instances = getApplicationInstances(client, app);

        boolean failOnCrashed = (boolean) context.getVariable(Constants.PARAM_FAIL_ON_CRASHED);

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

    private ExecutionStatus checkStartupStatus(DelegateExecution context, CloudFoundryOperations client, CloudApplication app,
        StartupStatus status) throws SLException {

        StepsUtil.saveAppLogs(context, client, recentLogsRetriever, app, LOGGER.getLoggerImpl(), processLoggerProviderFactory);
        if (status.equals(StartupStatus.CRASHED) || status.equals(StartupStatus.FLAPPING)) {
            // Application failed to start
            String message = format(Messages.ERROR_STARTING_APP_2, app.getName(), getMessageForStatus(status));
            onError(message);
            setRetryMessage(context, message);
            return ExecutionStatus.LOGICAL_RETRY;
        } else if (status.equals(StartupStatus.STARTED)) {
            // Application started successfully
            List<String> uris = app.getUris();
            if (uris.isEmpty()) {
                getStepLogger().info(Messages.APP_STARTED, app.getName());
            } else {
                String urls = CommonUtil.toCommaDelimitedString(uris, getProtocolPrefix());
                getStepLogger().info(Messages.APP_STARTED_URLS, app.getName(), urls);
            }
            return ExecutionStatus.SUCCESS;
        } else {
            // Application not started yet, wait and try again unless it's a timeout
            if (StepsUtil.hasTimedOut(context, () -> System.currentTimeMillis())) {
                String message = format(Messages.APP_START_TIMED_OUT, app.getName());
                onError(message);
                setRetryMessage(context, message);
                return ExecutionStatus.LOGICAL_RETRY;
            }
            return ExecutionStatus.RUNNING;
        }
    }

    protected String getMessageForStatus(StartupStatus status) {
        if (status.equals(StartupStatus.FLAPPING)) {
            return "Some instances are flapping";
        } else if (status.equals(StartupStatus.CRASHED)) {
            return "Some instances have crashed";
        } else {
            return null;
        }
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
        getStepLogger().info(message);
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

    @Override
    protected String getIndexVariable() {
        return Constants.VAR_APPS_INDEX;
    }

}
