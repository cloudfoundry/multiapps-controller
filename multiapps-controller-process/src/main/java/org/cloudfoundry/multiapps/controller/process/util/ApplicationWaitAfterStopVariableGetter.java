package org.cloudfoundry.multiapps.controller.process.util;

import java.time.Duration;
import java.util.Optional;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;

@Named
public class ApplicationWaitAfterStopVariableGetter {

    public boolean isAppStopDelayEnvVariableSet(ProcessContext context) {
        Optional<String> stopDelayFromEnv = extractDelayVariableFromEnv(context);
        return stopDelayFromEnv.isPresent();
    }

    private Optional<String> extractDelayVariableFromEnv(ProcessContext context) {
        CloudApplicationExtended appToProcess = context.getVariable(Variables.APP_TO_PROCESS);
        if (appToProcess == null) {
            return Optional.empty();
        }
        Optional<String> waitAfterStopFromAppToProcess = Optional.ofNullable(appToProcess.getEnv()
                                                                                         .get(Constants.VAR_WAIT_AFTER_APP_STOP));
        if (waitAfterStopFromAppToProcess.isPresent()) {
            return waitAfterStopFromAppToProcess;
        }
        CloudApplication existingApp = context.getVariable(Variables.EXISTING_APP);
        if (existingApp == null) {
            var env = context.getControllerClient()
                             .getApplicationEnvironment(appToProcess.getName());
            return Optional.ofNullable(env.get(Constants.VAR_WAIT_AFTER_APP_STOP));
        }
        var appEnv = context.getControllerClient()
                            .getApplicationEnvironment(existingApp.getGuid());
        return Optional.ofNullable(appEnv.get(Constants.VAR_WAIT_AFTER_APP_STOP));
    }

    public Duration getDelayDurationFromAppEnv(ProcessContext context) {
        Optional<String> stopDelayFromEnv = extractDelayVariableFromEnv(context);
        if (stopDelayFromEnv.isPresent()) {
            return processDurationFromAppEnv(stopDelayFromEnv.get(), context);
        }
        return Duration.ZERO;
    }

    private Duration processDurationFromAppEnv(String stopDelayFromEnv, ProcessContext context) {
        try {
            int delay = Integer.parseInt(stopDelayFromEnv);
            return convertDelayToSeconds(delay, context);
        } catch (NumberFormatException e) {
            context.getStepLogger()
                   .warnWithoutProgressMessage(Messages.COULD_NOT_PARSE_APP_STOP_DELAY_VAR, e.getMessage());
            return Duration.ZERO;
        }
    }

    private Duration convertDelayToSeconds(int delay, ProcessContext context) {
        if (delay > ApplicationConfiguration.DEFAULT_MAX_STOP_DELAY_IN_SECONDS) {
            context.getStepLogger()
                   .warnWithoutProgressMessage(Messages.DELAY_AFTER_APP_STOP_0_ABOVE_MAX_VALUE, delay);
            return Duration.ofSeconds(ApplicationConfiguration.DEFAULT_MAX_STOP_DELAY_IN_SECONDS);
        }
        Duration delayAfterAppStop = Duration.ofSeconds(delay);
        if (delayAfterAppStop.isNegative()) {
            context.getStepLogger()
                   .warnWithoutProgressMessage(Messages.DELAY_AFTER_APP_STOP_CANNOT_BE_NEGATIVE, delay);
            return Duration.ZERO;
        }
        return delayAfterAppStop;
    }

}
