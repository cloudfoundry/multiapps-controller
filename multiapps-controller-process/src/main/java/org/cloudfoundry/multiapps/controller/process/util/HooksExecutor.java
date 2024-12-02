package org.cloudfoundry.multiapps.controller.process.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.ListUtils;
import org.cloudfoundry.multiapps.controller.core.model.HookPhase;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.steps.StepPhase;
import org.cloudfoundry.multiapps.controller.process.steps.StepsUtil;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.Hook;
import org.cloudfoundry.multiapps.mta.model.Module;

public class HooksExecutor {

    private final HooksCalculator hooksCalculator;
    private final Module moduleToDeploy;
    private final ProcessContext context;

    public HooksExecutor(HooksCalculator hooksCalculator, Module moduleToDeploy, ProcessContext context) {
        this.hooksCalculator = hooksCalculator;
        this.moduleToDeploy = moduleToDeploy;
        this.context = context;
    }

    public List<Hook> determineBeforeStepHooks(StepPhase currentStepPhase) {
        if (!hooksCalculator.isInPreExecuteStepPhase(currentStepPhase) || moduleToDeploy == null) {
            return Collections.emptyList();
        }
        HooksWithPhases hooksWithPhases = hooksCalculator.calculateHooksForExecution(moduleToDeploy, currentStepPhase);
        return hooksWithPhases.getHooks();
    }

    public List<Hook> executeBeforeStepHooks(StepPhase currentStepPhase) {
        if (!hooksCalculator.isInPreExecuteStepPhase(currentStepPhase)) {
            return Collections.emptyList();
        }
        return executeHooks(currentStepPhase);
    }

    private List<Hook> executeHooks(StepPhase currentStepPhase) {
        if (moduleToDeploy == null) {
            return Collections.emptyList();
        }
        HooksWithPhases hooksWithPhases = hooksCalculator.calculateHooksForExecution(moduleToDeploy, currentStepPhase);
        Map<String, List<String>> alreadyExecutedHooksForModule = StepsUtil.getExecutedHooksForModule(context.getExecution(),
                                                                                                      moduleToDeploy.getName());
        updateExecutedHooksForModule(alreadyExecutedHooksForModule, hooksWithPhases.getHookPhases(), hooksWithPhases.getHooks());
        context.setVariable(Variables.HOOKS_FOR_EXECUTION, hooksWithPhases.getHooks());
        return hooksWithPhases.getHooks();
    }

    private void updateExecutedHooksForModule(Map<String, List<String>> alreadyExecutedHooks, List<HookPhase> currentHookPhasesForExecution,
                                              List<Hook> hooksForExecution) {
        Map<String, List<String>> result = new HashMap<>(alreadyExecutedHooks);
        updateExecutedHooks(result, currentHookPhasesForExecution, hooksForExecution);
        StepsUtil.setExecutedHooksForModule(context.getExecution(), moduleToDeploy.getName(), result);
    }

    private void updateExecutedHooks(Map<String, List<String>> alreadyExecutedHooks, List<HookPhase> currentHookPhasesForExecution,
                                     List<Hook> hooksForExecution) {
        for (Hook hook : hooksForExecution) {
            updateHook(alreadyExecutedHooks, currentHookPhasesForExecution, hook);
        }
    }

    private void updateHook(Map<String, List<String>> alreadyExecutedHooks, List<HookPhase> currentHookPhasesForExecution, Hook hook) {
        List<String> hookPhasesBasedOnCurrentHookPhase = getHookPhasesBasedOnCurrentHookPhase(currentHookPhasesForExecution,
                                                                                              hook.getPhases());
        alreadyExecutedHooks.merge(hook.getName(), hookPhasesBasedOnCurrentHookPhase, ListUtils::union);
    }

    private List<String> getHookPhasesBasedOnCurrentHookPhase(List<HookPhase> currentHookPhasesForExecution, List<String> hookPhases) {
        return hookPhases.stream()
                         .filter(phase -> currentHookPhasesForExecution.contains(HookPhase.fromString(phase)))
                         .toList();
    }

    public List<Hook> executeAfterStepHooks(StepPhase currentStepPhase) {
        if (!hooksCalculator.isInPostExecuteStepPhase(currentStepPhase)) {
            return Collections.emptyList();
        }
        return executeHooks(currentStepPhase);
    }

}
