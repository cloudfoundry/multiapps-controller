package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;

import org.cloudfoundry.multiapps.controller.core.model.HookPhase;

public interface BeforeStepHookPhaseProvider {

    List<HookPhase> getHookPhasesBeforeStep(ProcessContext context);
}
