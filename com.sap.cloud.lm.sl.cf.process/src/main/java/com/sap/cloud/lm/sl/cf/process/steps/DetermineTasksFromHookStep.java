package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Arrays;

import org.cloudfoundry.client.lib.domain.CloudTask;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.TaskHookParser;
import com.sap.cloud.lm.sl.mta.model.Hook;

@Component("determineTasksFromHookStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetermineTasksFromHookStep extends SyncFlowableStep {

    protected TaskHookParser hookParser = new TaskHookParser();

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        Hook hook = StepsUtil.getHookForExecution(execution.getContext());

        getStepLogger().info(Messages.EXECUTING_HOOK_0, hook.getName());

        CloudTask task = hookParser.parse(hook);
        StepsUtil.setTasksToExecute(execution.getContext(), Arrays.asList(task));

        return StepPhase.DONE;
    }
    
    @Override
    protected void onStepError(DelegateExecution context, Exception e) throws Exception {
        throw e;
    }

}
