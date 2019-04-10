package com.sap.cloud.lm.sl.cf.process.util;

import com.sap.cloud.lm.sl.cf.core.model.HookPhase;
import com.sap.cloud.lm.sl.cf.core.parser.hook.HookParser;
import com.sap.cloud.lm.sl.mta.model.Hook;

public interface HookExecutor {

    void executeHook(HookExecution hookExecution);

    HookParser getHookParser();

    public static class HookExecution {

        private HookPhase currentHookPhaseForExecution;
        private Hook currentHookToExecute;
        private String onCompleteHookMessage;

        public HookExecution(HookPhase currentHookPhaseForExecution, Hook currentHookToExecute, String onCompleteHookMessage) {
            this.currentHookPhaseForExecution = currentHookPhaseForExecution;
            this.currentHookToExecute = currentHookToExecute;
            this.onCompleteHookMessage = onCompleteHookMessage;
        }

        public HookPhase getCurrentHookPhaseForExecution() {
            return currentHookPhaseForExecution;
        }

        public Hook getHook() {
            return currentHookToExecute;
        }

        public String getOnCompleteHookMessage() {
            return onCompleteHookMessage;
        }

    }
}
