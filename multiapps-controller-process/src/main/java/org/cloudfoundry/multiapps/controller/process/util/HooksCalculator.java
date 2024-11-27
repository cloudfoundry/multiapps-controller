package org.cloudfoundry.multiapps.controller.process.util;

import java.util.Collections;
import java.util.List;

import org.cloudfoundry.multiapps.controller.core.model.HookPhase;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.steps.StepPhase;
import org.cloudfoundry.multiapps.mta.model.Hook;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.immutables.value.Value;

@Value.Immutable
public abstract class HooksCalculator {

    public HooksWithPhases calculateHooksForExecution(Module moduleToDeploy, StepPhase currentStepPhase) {
        List<HookPhase> currentHookPhasesForExecution = determineHookPhaseForCurrentStepPhase(currentStepPhase);
        return ImmutableHooksWithPhases.builder()
                                       .hooks(getHooksForCurrentPhase(moduleToDeploy, currentHookPhasesForExecution))
                                       .hookPhases(currentHookPhasesForExecution)
                                       .build();
    }

    private List<HookPhase> determineHookPhaseForCurrentStepPhase(StepPhase currentStepPhase) {
        if (isInPreExecuteStepPhase(currentStepPhase)) {
            return getHookPhasesBeforeStep();
        }
        if (isInPostExecuteStepPhase(currentStepPhase)) {
            return getHookPhasesAfterStep();
        }
        return Collections.singletonList(HookPhase.NONE);
    }

    boolean isInPreExecuteStepPhase(StepPhase currentStepPhase) {
        return currentStepPhase == StepPhase.EXECUTE || currentStepPhase == StepPhase.RETRY;
    }

    boolean isInPostExecuteStepPhase(StepPhase currentStepPhase) {
        return currentStepPhase == StepPhase.DONE;
    }

    private List<Hook> getHooksForCurrentPhase(Module moduleToDeploy, List<HookPhase> currentHookPhasesForExecution) {
        return getModuleHooksAggregator(moduleToDeploy).aggregateHooks(currentHookPhasesForExecution);
    }

    private ModuleHooksAggregator getModuleHooksAggregator(Module moduleToDeploy) {
        return new ModuleHooksAggregator(getContext().getExecution(), moduleToDeploy);
    }

    public abstract ProcessContext getContext();

    public abstract List<HookPhase> getHookPhasesBeforeStep();

    public abstract List<HookPhase> getHookPhasesAfterStep();
}
