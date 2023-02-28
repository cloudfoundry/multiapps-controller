package org.cloudfoundry.multiapps.controller.process.steps;

import static java.text.MessageFormat.format;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.core.util.UriUtil;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLoggerProvider;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudRoute;
import com.sap.cloudfoundry.client.facade.domain.InstanceInfo;
import com.sap.cloudfoundry.client.facade.domain.InstanceState;

public class PollStartAppStatusExecution implements AsyncExecution {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollStartAppStatusExecution.class);

    enum StartupStatus {
        STARTING, STARTED, CRASHED, DOWN
    }

    @Override
    public AsyncExecutionState execute(ProcessContext context) {
        String appToPoll = getAppToPoll(context).getName();
        CloudControllerClient client = context.getControllerClient();

        context.getStepLogger()
               .debug(Messages.CHECKING_APP_STATUS, appToPoll);

        CloudApplication app = getApplication(context, appToPoll, client);
        List<InstanceInfo> appInstances = client.getApplicationInstances(app)
                                                .getInstances();
        StartupStatus status = getStartupStatus(context, app.getName(), appInstances);
        ProcessLoggerProvider processLoggerProvider = context.getStepLogger()
                                                             .getProcessLoggerProvider();
        StepsUtil.saveAppLogs(context, client, app.getName(), LOGGER, processLoggerProvider);
        return checkStartupStatus(context, app, status);
    }

    public String getPollingErrorMessage(ProcessContext context) {
        String appToPoll = getAppToPoll(context).getName();
        return format(Messages.ERROR_STARTING_APP_0, appToPoll);
    }

    protected CloudApplication getAppToPoll(ProcessContext context) {
        return context.getVariable(Variables.APP_TO_PROCESS);
    }

    private CloudApplication getApplication(ProcessContext context, String appToPoll, CloudControllerClient client) {
        CloudApplication application = context.getVariable(Variables.EXISTING_APP_TO_POLL);
        if (application == null) {
            application = client.getApplication(appToPoll);
            context.setVariable(Variables.EXISTING_APP_TO_POLL, application);
        }
        return application;
    }

    private StartupStatus getStartupStatus(ProcessContext context, String appName, List<InstanceInfo> appInstances) {
        boolean failOnCrashed = context.getVariable(Variables.FAIL_ON_CRASHED);

        int expectedInstances = appInstances.size();
        long runningInstances = getInstanceCount(appInstances, InstanceState.RUNNING);
        long downInstances = getInstanceCount(appInstances, InstanceState.DOWN);
        long crashedInstances = getInstanceCount(appInstances, InstanceState.CRASHED);
        long startingInstances = getInstanceCount(appInstances, InstanceState.STARTING);

        String states = composeStatesMessage(appInstances);

        context.getStepLogger()
               .debug(Messages.APPLICATION_0_X_OF_Y_INSTANCES_RUNNING, appName, runningInstances, expectedInstances, states);

        if (runningInstances == expectedInstances) {
            return StartupStatus.STARTED;
        }
        if (startingInstances > 0) {
            return StartupStatus.STARTING;
        }
        if (downInstances > 0) {
            return StartupStatus.DOWN;
        }
        if (crashedInstances > 0 && failOnCrashed) {
            return StartupStatus.CRASHED;
        }
        return StartupStatus.STARTING;
    }

    private AsyncExecutionState checkStartupStatus(ProcessContext context, CloudApplication app, StartupStatus status) {
        if (status == StartupStatus.CRASHED) {
            onError(context, Messages.ERROR_STARTING_APP_0_DESCRIPTION_1, app.getName(), Messages.SOME_INSTANCES_HAVE_CRASHED);
            return AsyncExecutionState.ERROR;
        }
        if (status == StartupStatus.DOWN) {
            onError(context, Messages.ERROR_STARTING_APP_0_DESCRIPTION_1, app.getName(), Messages.SOME_INSTANCES_ARE_DOWN);
            return AsyncExecutionState.ERROR;
        }
        if (status == StartupStatus.STARTED) {
            List<CloudRoute> routes = context.getControllerClient()
                                             .getApplicationRoutes(app.getGuid());
            if (routes.isEmpty()) {
                context.getStepLogger()
                       .info(Messages.APP_STARTED, app.getName());
            } else {
                context.getStepLogger()
                       .info(Messages.APP_STARTED_URLS, app.getName(), UriUtil.prettyPrintRoutes(routes));
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
            stateCounts = Map.of(InstanceState.STARTING.toString(), 0L);
        } else {
            stateCounts = instances.stream()
                                   .collect(Collectors.groupingBy(instance -> instance.getState()
                                                                                      .toString(),
                                                                  Collectors.counting()));
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

    private long getInstanceCount(List<InstanceInfo> instances, InstanceState state) {
        return instances.stream()
                        .filter(instance -> instance.getState()
                                                    .equals(state))
                        .count();
    }
}
