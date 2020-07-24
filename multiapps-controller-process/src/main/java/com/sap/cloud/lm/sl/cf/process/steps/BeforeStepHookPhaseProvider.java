package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import com.sap.cloud.lm.sl.cf.core.model.HookPhase;

public interface BeforeStepHookPhaseProvider {

    List<HookPhase> getHookPhasesBeforeStep(ProcessContext context);
}
