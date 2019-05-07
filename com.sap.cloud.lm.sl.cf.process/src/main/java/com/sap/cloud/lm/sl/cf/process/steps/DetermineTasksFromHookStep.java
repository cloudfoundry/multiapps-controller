package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudTask;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.TasksHookParser;
import com.sap.cloud.lm.sl.mta.model.Hook;

@Component("determineTasksFromHookStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetermineTasksFromHookStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws Exception {
        Hook hookForExecution = StepsUtil.getHookForExecution(execution.getContext());

        getStepLogger().info(Messages.EXECUTING_HOOK_0, hookForExecution.getName());

        List<CloudTask> tasksFromHook = new TasksHookParser().parse(hookForExecution);
        StepsUtil.setTasksToExecute(execution.getContext(), tasksFromHook);

        return StepPhase.DONE;
    }

}
