
package com.sap.cloud.lm.sl.cf.process.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.core.model.HookPhase;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.mta.model.Hook;
import com.sap.cloud.lm.sl.mta.model.Module;

public class ModuleHooksAggregator {

    private static final byte MAJOR_SCHEMA_VERSION_THREE = 3;

    private final DelegateExecution execution;
    private final Module moduleToDeploy;

    public ModuleHooksAggregator(DelegateExecution execution, Module moduleToDeploy) {
        this.execution = execution;
        this.moduleToDeploy = moduleToDeploy;
    }

    public List<Hook> aggregateHooks(List<HookPhase> currentHookPhasesForExecution) {
        Map<String, List<String>> alreadyExecutedHooksForModule = getAlreadyExecutedHooks();
        List<Hook> hooksCalculatedForExecution = determineHooksForExecution(alreadyExecutedHooksForModule, currentHookPhasesForExecution);
        updateExecutedHooksForModule(alreadyExecutedHooksForModule, currentHookPhasesForExecution, hooksCalculatedForExecution);
        return hooksCalculatedForExecution;
    }

    private Map<String, List<String>> getAlreadyExecutedHooks() {
        return StepsUtil.getExecutedHooksForModule(execution, moduleToDeploy.getName());
    }

    private List<Hook> determineHooksForExecution(Map<String, List<String>> alreadyExecutedHooks,
                                                  List<HookPhase> hookPhasesForCurrentStepPhase) {
        List<Hook> moduleHooksToExecuteOnCurrentStepPhase = collectHooksWithPhase(moduleToDeploy, hookPhasesForCurrentStepPhase);
        return getHooksForExecution(alreadyExecutedHooks, moduleHooksToExecuteOnCurrentStepPhase, hookPhasesForCurrentStepPhase);
    }

    private List<Hook> collectHooksWithPhase(Module moduleToDeploy, List<HookPhase> hookPhasesForCurrentStepPhase) {
        return getModuleHooks(moduleToDeploy).stream()
                                             .filter(hook -> shouldCollectHook(hook.getPhases(), hookPhasesForCurrentStepPhase))
                                             .collect(Collectors.toList());
    }

    private List<Hook> getModuleHooks(Module moduleToDeploy) {
        return moduleToDeploy.getMajorSchemaVersion() < MAJOR_SCHEMA_VERSION_THREE ? Collections.emptyList() : moduleToDeploy.getHooks();
    }

    private boolean shouldCollectHook(List<String> hookPhases, List<HookPhase> hookTypeForCurrentStepPhase) {
        List<HookPhase> resolvedHookPhases = mapToHookPhases(hookPhases);
        return resolvedHookPhases.stream()
                                 .anyMatch(hookTypeForCurrentStepPhase::contains);
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
        List<HookPhase> modifiedHookPhasesForHook = getModifiedHookPhasesForCurrentStepPhase(hookToBeExecuted,
                                                                                             hookPhasesForCurrentStepPhase);
        return executedHookPhasesForHook.containsAll(modifiedHookPhasesForHook);
    }

    @Deprecated
    private List<HookPhase> getModifiedHookPhasesForCurrentStepPhase(Hook hookToBeExecuted, List<HookPhase> hookPhasesForCurrentStepPhase) {
        if (doesHookContainOldPhases(hookToBeExecuted)) {
            return hookPhasesForCurrentStepPhase.stream()
                                                .filter(this::containsOldPhase)
                                                .collect(Collectors.toList());
        }
        return hookPhasesForCurrentStepPhase.stream()
                                            .filter(hookPhase -> !containsOldPhase(hookPhase))
                                            .collect(Collectors.toList());
    }

    private boolean doesHookContainOldPhases(Hook hookToBeExecuted) {
        return hookToBeExecuted.getPhases()
                               .stream()
                               .anyMatch(hookPhase -> containsOldPhase(HookPhase.fromString(hookPhase)));
    }

    private boolean containsOldPhase(HookPhase hookPhase) {
        return HookPhase.getOldPhases()
                        .contains(hookPhase);
    }

    private List<HookPhase> getExecutedHookPhasesForHook(Map<String, List<String>> alreadyExecutedHooks, String hookName) {
        List<String> executedHookPhasesForHook = alreadyExecutedHooks.get(hookName);
        return mapToHookPhases(executedHookPhasesForHook);
    }

    private void updateExecutedHooksForModule(Map<String, List<String>> alreadyExecutedHooks, List<HookPhase> currentHookPhasesForExecution,
                                              List<Hook> hooksForExecution) {
        Map<String, List<String>> result = new HashMap<>(alreadyExecutedHooks);
        updateExecutedHooks(result, currentHookPhasesForExecution, hooksForExecution);
        StepsUtil.setExecutedHooksForModule(execution, moduleToDeploy.getName(), result);
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
                         .collect(Collectors.toList());
    }

}