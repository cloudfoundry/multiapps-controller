package com.sap.cloud.lm.sl.cf.process.util;

import java.text.MessageFormat;

import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.core.flowable.FlowableFacade;

public class HooksExecutorFactory {

    public HookExecutor getHookExecutor(DelegateExecution context, FlowableFacade flowableFacade, String hookType) {
        if (hookType.equals(TasksHookExecutor.HOOK_TYPE_TASKS)) {
            return new TasksHookExecutor(context, flowableFacade);
        }

        throw new IllegalStateException(MessageFormat.format("Unsupported hook type \"{0}\"", hookType));
    }
}
