package org.cloudfoundry.multiapps.controller.process.util;

import java.util.Collections;
import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.model.HookPhase;
import org.cloudfoundry.multiapps.controller.process.steps.AfterStepHookPhaseProvider;
import org.cloudfoundry.multiapps.controller.process.steps.BeforeStepHookPhaseProvider;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.steps.SyncFlowableStep;

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
