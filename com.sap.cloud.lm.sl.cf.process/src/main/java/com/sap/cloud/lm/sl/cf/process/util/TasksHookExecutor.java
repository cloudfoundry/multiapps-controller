package com.sap.cloud.lm.sl.cf.process.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudTask;
import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.core.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.core.model.HookPhase;
import com.sap.cloud.lm.sl.cf.core.parser.TaskParametersParser;
import com.sap.cloud.lm.sl.cf.core.parser.hook.HookParser;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;

public class TasksHookExecutor implements HookExecutor {

    public static final String HOOK_TYPE_TASKS = "tasks";

    private DelegateExecution context;
    private FlowableFacade flowableFacade;

    public TasksHookExecutor(DelegateExecution context, FlowableFacade flowableFacade) {
        this.context = context;
        this.flowableFacade = flowableFacade;
    }

    @Override
    public void executeHook(HookExecution hookExecution) {
        Map<String, Object> executionVariables = prepareExecutionVariables(context, hookExecution);
        Map<String, Object> allProcessVariables = MapUtil.merge(context.getVariables(), executionVariables);
        executeHook(hookExecution.getCurrentHookPhaseForExecution(), allProcessVariables);
    }

    private void executeHook(HookPhase currentHookPhaseForExecution, Map<String, Object> processVariables) {
        flowableFacade.startProcessByMessage(currentHookPhaseForExecution.toString(), processVariables);
    }

    private Map<String, Object> prepareExecutionVariables(DelegateExecution context, HookExecution hookExecution) {
        Map<String, Object> executionVariables = new HashMap<>();
        executionVariables.put(Constants.VAR_ON_COMPLETE_MESSAGE_EVENT_NAME, hookExecution.getOnCompleteHookMessage());
        executionVariables.put(Constants.VAR_PARENT_EXECUTION_ID, context.getId());

        List<CloudTask> tasksFromHook = getHookParser().parseParameters(hookExecution.getHook()
            .getParameters());
        executionVariables.put(Constants.VAR_TASKS_TO_EXECUTE, JsonUtil.toJsonBinary(tasksFromHook));
        return executionVariables;
    }

    @Override
    public HookParser<List<CloudTask>> getHookParser() {
        return new TasksHookParser();
    }

    private class TasksHookParser implements HookParser<List<CloudTask>> {

        @Override
        public List<CloudTask> parseParameters(Map<String, Object> hookParameters) {
            if (hookParameters.isEmpty()) {
                throw new IllegalStateException("Hook task parameters must not be empty");
            }
            return Arrays.asList(getHookTask(hookParameters));
        }

        private CloudTask getHookTask(Map<String, Object> hookParameters) {
            return new TaskParametersParser.CloudTaskMapper().toCloudTask(hookParameters);
        }

    }

}
