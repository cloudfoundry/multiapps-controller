package com.sap.cloud.lm.sl.cf.process.util;

import java.util.Collections;
import java.util.List;

import javax.inject.Named;

import com.sap.cloud.lm.sl.cf.core.model.HookPhase;
import com.sap.cloud.lm.sl.cf.process.steps.AfterStepHookPhaseProvider;
import com.sap.cloud.lm.sl.cf.process.steps.BeforeStepHookPhaseProvider;
import com.sap.cloud.lm.sl.cf.process.steps.ProcessContext;
import com.sap.cloud.lm.sl.cf.process.steps.SyncFlowableStep;

@Named
public class HooksPhaseGetter {

    public List<HookPhase> getHookPhasesBeforeStop(SyncFlowableStep syncFlowableStep, ProcessContext context) {
        if (syncFlowableStep instanceof BeforeStepHookPhaseProvider) {
            return ((BeforeStepHookPhaseProvider) syncFlowableStep).getHookPhasesBeforeStep(context);
        }
        return Collections.singletonList(HookPhase.NONE);
    }

    public List<HookPhase> getHookPhasesAfterStop(SyncFlowableStep syncFlowableStep, ProcessContext context) {
        if (syncFlowableStep instanceof AfterStepHookPhaseProvider) {
            return ((AfterStepHookPhaseProvider) syncFlowableStep).getHookPhasesAfterStep(context);
        }
        return Collections.singletonList(HookPhase.NONE);
    }
}
