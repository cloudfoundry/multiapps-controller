package com.sap.cloud.lm.sl.cf.process.util;

import java.util.Collections;
import java.util.List;

import org.cloudfoundry.multiapps.mta.model.Hook;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.immutables.value.Value;

import com.sap.cloud.lm.sl.cf.core.model.HookPhase;
import com.sap.cloud.lm.sl.cf.process.steps.ProcessContext;
import com.sap.cloud.lm.sl.cf.process.steps.StepPhase;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

@Value.Immutable
public abstract class HooksCalculator {

    public List<Hook> calculateHooksForExecution(Module moduleToDeploy, StepPhase currentStepPhase) {
        List<HookPhase> currentHookPhasesForExecution = determineHookPhaseForCurrentStepPhase(currentStepPhase);
        return getHooksForCurrentPhase(moduleToDeploy, currentHookPhasesForExecution);
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

    public void setHooksForExecution(List<Hook> hooksForExecution) {
        getContext().setVariable(Variables.HOOKS_FOR_EXECUTION, hooksForExecution);
    }

    public abstract ProcessContext getContext();

    public abstract List<HookPhase> getHookPhasesBeforeStep();

    public abstract List<HookPhase> getHookPhasesAfterStep();
}
