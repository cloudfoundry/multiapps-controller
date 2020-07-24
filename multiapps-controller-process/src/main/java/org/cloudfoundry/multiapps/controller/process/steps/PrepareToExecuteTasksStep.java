package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.client.lib.domain.CloudTask;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("prepareToExecuteTasksStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PrepareToExecuteTasksStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        List<CloudTask> tasksToExecute = context.getVariable(Variables.TASKS_TO_EXECUTE);
        context.setVariable(Variables.TASKS_COUNT, tasksToExecute.size());
        context.setVariable(Variables.TASKS_INDEX, 0);
        context.setVariable(Variables.INDEX_VARIABLE_NAME, Variables.TASKS_INDEX.getName());
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format(Messages.ERROR_PREPARING_TO_EXECUTE_TASKS_ON_APP, context.getVariable(Variables.APP_TO_PROCESS)
                                                                                             .getName());
    }

}
