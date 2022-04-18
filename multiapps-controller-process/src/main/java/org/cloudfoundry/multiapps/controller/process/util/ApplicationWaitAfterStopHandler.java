package org.cloudfoundry.multiapps.controller.process.util;

import java.time.Duration;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

@Named
public class ApplicationWaitAfterStopHandler {

    protected ApplicationWaitAfterStopVariableGetter delayVariableGetter;

    @Inject
    public ApplicationWaitAfterStopHandler(ApplicationWaitAfterStopVariableGetter delayVariableGetter) {
        this.delayVariableGetter = delayVariableGetter;
    }

    public void configureDelayAfterAppStop(ProcessContext context, String appName) {
        boolean shouldWaitAfterAppStop = shouldWaitAfterAppStop(context);
        if (shouldWaitAfterAppStop) {
            setWaitAfterAppStopVariable(context, appName);
            context.getStepLogger()
                   .info(Messages.DELAYING_APP_0_FOR_1_SECONDS, appName, context.getVariable(Variables.DELAY_AFTER_APP_STOP)
                                                                                .toSeconds());
        }
    }

    private boolean shouldWaitAfterAppStop(ProcessContext context) {
        if (!delayVariableGetter.isAppStopDelayEnvVariableSet(context)) {
            return false;
        }
        Duration durationToWait = delayVariableGetter.getDelayDurationFromAppEnv(context);
        return !durationToWait.isZero();
    }

    private void setWaitAfterAppStopVariable(ProcessContext context, String appName) {
        Duration durationToWait = delayVariableGetter.getDelayDurationFromAppEnv(context);
        context.getStepLogger()
               .debug(Messages.SETTING_WAIT_AFTER_STOP_FOR_APP_0_TO_1_SECONDS, appName, durationToWait.toSeconds());
        context.setVariable(Variables.DELAY_AFTER_APP_STOP, durationToWait);
    }

}
