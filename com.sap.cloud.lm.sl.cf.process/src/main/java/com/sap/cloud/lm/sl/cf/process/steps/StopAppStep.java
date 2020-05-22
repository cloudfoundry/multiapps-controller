package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.State;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.core.model.HookPhase;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ProcessTypeParser;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;

@Named("stopAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class StopAppStep extends SyncFlowableStepWithHooks implements BeforeStepHookPhaseProvider, AfterStepHookPhaseProvider {

    @Inject
    private ProcessTypeParser processTypeParser;

    @Override
    public StepPhase executeStepInternal(ProcessContext context) {
        // Get the next cloud application from the context
        CloudApplication app = context.getVariable(Variables.APP_TO_PROCESS);

        // Get the existing application from the context
        CloudApplication existingApp = context.getVariable(Variables.EXISTING_APP);

        if (existingApp != null && !existingApp.getState()
                                               .equals(State.STOPPED)) {
            getStepLogger().info(Messages.STOPPING_APP, app.getName());

            // Get a cloud foundry client
            CloudControllerClient client = context.getControllerClient();

            // Stop the application
            client.stopApplication(app.getName());

            getStepLogger().debug(Messages.APP_STOPPED, app.getName());
        } else {
            getStepLogger().debug("Application \"{0}\" already stopped", app.getName());
        }
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format(Messages.ERROR_STOPPING_APP, context.getVariable(Variables.APP_TO_PROCESS)
                                                                        .getName());
    }

    @Override
    public List<HookPhase> getHookPhasesBeforeStep(ProcessContext context) {
        ProcessType processType = processTypeParser.getProcessType(context.getExecution());
        if (ProcessType.BLUE_GREEN_DEPLOY.equals(processType)) {
            return Collections.singletonList(HookPhase.APPLICATION_BEFORE_STOP_IDLE);
        }

        return Collections.singletonList(HookPhase.APPLICATION_BEFORE_STOP_LIVE);
    }

    @Override
    public List<HookPhase> getHookPhasesAfterStep(ProcessContext context) {
        ProcessType processType = processTypeParser.getProcessType(context.getExecution());
        if (ProcessType.BLUE_GREEN_DEPLOY.equals(processType)) {
            return Collections.singletonList(HookPhase.APPLICATION_AFTER_STOP_IDLE);
        }

        return Collections.singletonList(HookPhase.APPLICATION_AFTER_STOP_LIVE);
    }

}
