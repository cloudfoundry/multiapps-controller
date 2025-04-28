package org.cloudfoundry.multiapps.controller.process.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.core.model.HookPhase;
import org.cloudfoundry.multiapps.controller.process.steps.StepsUtil;
import org.cloudfoundry.multiapps.mta.model.Hook;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.flowable.engine.delegate.DelegateExecution;

public class ModuleHooksAggregator {
    private static final int MAJOR_SCHEMA_VERSION_THREE = 3;
    private final DelegateExecution execution;
    private final Module moduleToDeploy;

    public ModuleHooksAggregator(DelegateExecution execution, Module moduleToDeploy) {
        this.execution = execution;
        this.moduleToDeploy = moduleToDeploy;
    }

    public List<Hook> aggregateHooks(List<HookPhase> currentHookPhasesForExecution) {
        Map<String, List<String>> alreadyExecutedHooksForModule = StepsUtil.getExecutedHooksForModule(execution, moduleToDeploy.getName());
        return determineHooksForExecution(alreadyExecutedHooksForModule, currentHookPhasesForExecution);
    }

    private List<Hook> determineHooksForExecution(Map<String, List<String>> alreadyExecutedHooks,
                                                  List<HookPhase> hookPhasesForCurrentStepPhase) {
        List<Hook> moduleHooksToExecuteOnCurrentStepPhase = collectHooksWithPhase(hookPhasesForCurrentStepPhase);
        return getHooksForExecution(alreadyExecutedHooks, moduleHooksToExecuteOnCurrentStepPhase, hookPhasesForCurrentStepPhase);
    }

    private List<Hook> collectHooksWithPhase(List<HookPhase> hookPhasesForCurrentStepPhase) {
        return getModuleHooks().stream()
                               .filter(hook -> shouldCollectHook(hook.getPhases(), hookPhasesForCurrentStepPhase))
                               .collect(Collectors.toList());
    }

    private List<Hook> getModuleHooks() {
        return moduleToDeploy.getMajorSchemaVersion() < MAJOR_SCHEMA_VERSION_THREE ? Collections.emptyList() : moduleToDeploy.getHooks();
    }

    private boolean shouldCollectHook(List<String> hookPhases, List<HookPhase> hookTypeForCurrentStepPhase) {
        List<HookPhase> resolvedHookPhases = mapToHookPhases(hookPhases);
        return !Collections.disjoint(resolvedHookPhases, hookTypeForCurrentStepPhase);
    }

    private List<HookPhase> mapToHookPhases(List<String> hookPhases) {
        return hookPhases.stream()
                         .map(HookPhase::fromString)
                         .collect(Collectors.toList());
    }

    private List<Hook> getHooksForExecution(Map<String, List<String>> alreadyExecutedHooks, List<Hook> moduleHooksToBeExecuted,
                                            List<HookPhase> hookPhasesForCurrentStepPhase) {
        return moduleHooksToBeExecuted.stream()
                                      .filter(hook -> shouldExecuteHook(alreadyExecutedHooks, hookPhasesForCurrentStepPhase, hook))
                                      .collect(Collectors.toList());
    }

    private boolean shouldExecuteHook(Map<String, List<String>> alreadyExecutedHooks, List<HookPhase> hookPhasesForCurrentStepPhase,
                                      Hook hook) {
        return !alreadyExecutedHooks.containsKey(hook.getName())
            || !hasAllPhasesExecuted(alreadyExecutedHooks, hook, hookPhasesForCurrentStepPhase);
    }

    private boolean hasAllPhasesExecuted(Map<String, List<String>> alreadyExecutedHooks, Hook hookToBeExecuted,
                                         List<HookPhase> hookPhasesForCurrentStepPhase) {
        List<HookPhase> executedHookPhasesForHook = getExecutedHookPhasesForHook(alreadyExecutedHooks, hookToBeExecuted.getName());
        return executedHookPhasesForHook.containsAll(hookPhasesForCurrentStepPhase);
    }

    private List<HookPhase> getExecutedHookPhasesForHook(Map<String, List<String>> alreadyExecutedHooks, String hookName) {
        List<String> executedHookPhasesForHook = alreadyExecutedHooks.get(hookName);
        return mapToHookPhases(executedHookPhasesForHook);
    }

}