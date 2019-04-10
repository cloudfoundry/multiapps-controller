package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.ListUtils;
import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.core.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.core.model.HookPhase;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.HookExecutor;
import com.sap.cloud.lm.sl.cf.process.util.HookExecutor.HookExecution;
import com.sap.cloud.lm.sl.cf.process.util.HooksExecutorFactory;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;
import com.sap.cloud.lm.sl.mta.model.Hook;
import com.sap.cloud.lm.sl.mta.model.Module;

public abstract class SyncFlowableStepWithHooks extends SyncFlowableStep {

    private static final String NO_ON_COMPLETE_HOOK_MESSAGE_NAME = "noOnCompleteMessage";

    @Inject
    private FlowableFacade flowableFacade;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws Exception {
        StepPhase currentStepPhase = StepsUtil.getStepPhase(execution.getContext());
        Module moduleToDeploy = StepsUtil.getModuleToDeploy(execution.getContext());

        List<Hook> executedHooks = executeHooksForStepPhase(execution.getContext(), moduleToDeploy, currentStepPhase);
        if (!executedHooks.isEmpty()) {
            return currentStepPhase;
        }

        currentStepPhase = executeStepInternal(execution);

        if (!isInPostExecuteStepPhase(currentStepPhase)) {
            return currentStepPhase;
        }
        executeHooksForStepPhase(execution.getContext(), moduleToDeploy, currentStepPhase);

        return currentStepPhase;
    }

    private List<Hook> executeHooksForStepPhase(DelegateExecution context, Module moduleToDeploy, StepPhase currentStepPhase) {
        HookPhase currentHookPhaseForExecution = determineHookPhaseForCurrentStepPhase(currentStepPhase);
        List<Hook> hooksForCurrentPhase = getHooksForCurrentPhase(context, moduleToDeploy, currentHookPhaseForExecution);
        executeHooks(context, hooksForCurrentPhase, currentHookPhaseForExecution);

        return hooksForCurrentPhase;
    }

    private List<Hook> getHooksForCurrentPhase(DelegateExecution context, Module moduleToDeploy, HookPhase currentHookPhaseForExecution) {
        return getModuleHooksAggregator(context, moduleToDeploy).aggregateHooks(currentHookPhaseForExecution);
    }

    protected ModuleHooksAggregator getModuleHooksAggregator(DelegateExecution context, Module moduleToDeploy) {
        return new ModuleHooksAggregator(context, moduleToDeploy);
    }

    private void executeHooks(DelegateExecution context, List<Hook> hooksToExecute, HookPhase currentHookPhaseForExecution) {
        getHooksExecutor(context).executeHooks(currentHookPhaseForExecution, hooksToExecute);
    }

    protected HooksExecutor getHooksExecutor(DelegateExecution context) {
        return new HooksExecutor(context, flowableFacade, getStepLogger());
    }

    private boolean isInPreExecuteStepPhase(StepPhase currentStepPhase) {
        return currentStepPhase == StepPhase.EXECUTE || currentStepPhase == StepPhase.RETRY;
    }

    private boolean isInPostExecuteStepPhase(StepPhase currentStepPhase) {
        return currentStepPhase == StepPhase.DONE;
    }

    protected HookPhase getHookPhaseBeforeStep() {
        return HookPhase.NONE;
    }

    protected HookPhase getHookPhaseAfterStep() {
        return HookPhase.NONE;
    }

    protected String getOnCompleteHookMessageName() {
        return NO_ON_COMPLETE_HOOK_MESSAGE_NAME;
    }

    private HookPhase determineHookPhaseForCurrentStepPhase(StepPhase currentStepPhase) {
        if (isInPreExecuteStepPhase(currentStepPhase)) {
            return getHookPhaseBeforeStep();
        }
        if (isInPostExecuteStepPhase(currentStepPhase)) {
            return getHookPhaseAfterStep();
        }
        return HookPhase.NONE;
    }

    protected abstract StepPhase executeStepInternal(ExecutionWrapper execution) throws Exception;

    class ModuleHooksAggregator {

        private DelegateExecution context;
        private Module moduleToDeploy;

        public ModuleHooksAggregator(DelegateExecution context, Module moduleToDeploy) {
            this.context = context;
            this.moduleToDeploy = moduleToDeploy;
        }

        public List<Hook> aggregateHooks(HookPhase currentHookPhaseForExecution) {
            Map<String, List<String>> alreadyExecutedHooksForModule = getAlreadyExecutedHooks();
            List<Hook> hooksCalculatedForExecution = determineHooksForExecution(alreadyExecutedHooksForModule,
                currentHookPhaseForExecution);
            updateExecutedHooksForModule(alreadyExecutedHooksForModule, currentHookPhaseForExecution, hooksCalculatedForExecution);
            return hooksCalculatedForExecution;
        }

        private Map<String, List<String>> getAlreadyExecutedHooks() {
            return StepsUtil.getExecutedHooksForModule(context, moduleToDeploy.getName());
        }

        private List<Hook> determineHooksForExecution(Map<String, List<String>> alreadyExecutedHooks,
            HookPhase hookPhaseForCurrentStepPhase) {
            List<Hook> moduleHooksToExecuteOnCurrentStepPhase = collectHooksWithPhase(moduleToDeploy, hookPhaseForCurrentStepPhase);
            return getHooksForExecution(alreadyExecutedHooks, moduleHooksToExecuteOnCurrentStepPhase, hookPhaseForCurrentStepPhase);
        }

        private List<Hook> collectHooksWithPhase(Module moduleToDeploy, HookPhase hookTypeForCurrentStepPhase) {
            return getModuleHooks(moduleToDeploy).stream()
                .filter(hook -> mapToHookPhases(hook.getPhases()).contains(hookTypeForCurrentStepPhase))
                .collect(Collectors.toList());
        }

        private List<Hook> getModuleHooks(Module moduleToDeploy) {
            if (moduleToDeploy.getMajorSchemaVersion() < 3) {
                return Collections.emptyList();
            }

            return moduleToDeploy.getHooks();
        }

        private List<HookPhase> mapToHookPhases(List<String> hookPhases) {
            return hookPhases.stream()
                .map(HookPhase::fromString)
                .collect(Collectors.toList());
        }

        private List<Hook> getHooksForExecution(Map<String, List<String>> alreadyExecutedHooks, List<Hook> moduleHooksToBeExecuted,
            HookPhase hookPhaseForCurrentStepPhase) {

            List<Hook> notExecutedHooks = moduleHooksToBeExecuted.stream()
                .filter(hook -> !alreadyExecutedHooks.containsKey(hook.getName()))
                .collect(Collectors.toList());

            List<Hook> hooksWithNonExecutedPhases = moduleHooksToBeExecuted.stream()
                .filter(hookToBeExecuted -> alreadyExecutedHooks.containsKey(hookToBeExecuted.getName()))
                .filter(hookToBeExecuted -> !getExecutedHookPhasesForHook(alreadyExecutedHooks, hookToBeExecuted.getName())
                    .contains(hookPhaseForCurrentStepPhase))
                .collect(Collectors.toList());

            return ListUtils.union(notExecutedHooks, hooksWithNonExecutedPhases);
        }

        private List<HookPhase> getExecutedHookPhasesForHook(Map<String, List<String>> alreadyExecutedHooks, String hookName) {
            List<String> executedHookPhasesForHook = alreadyExecutedHooks.get(hookName);
            return mapToHookPhases(executedHookPhasesForHook);
        }

        private void updateExecutedHooksForModule(Map<String, List<String>> alreadyExecutedHooks, HookPhase currentHookPhaseForExecution,
            List<Hook> hooksForExecution) {
            Map<String, List<String>> result = new HashMap<>(alreadyExecutedHooks);
            updateExecutedHooks(result, currentHookPhaseForExecution, hooksForExecution);
            StepsUtil.setExecutedHooksForModule(context, moduleToDeploy.getName(), result);
        }

        private void updateExecutedHooks(Map<String, List<String>> alreadyExecutedHooks, HookPhase currentHookPhaseForExecution,
            List<Hook> hooksForExecution) {
            hooksForExecution.forEach(hook -> updateHook(alreadyExecutedHooks, currentHookPhaseForExecution, hook));
        }

        private void updateHook(Map<String, List<String>> alreadyExecutedHooks, HookPhase currentHookPhaseForExecution, Hook hook) {
            List<String> hookPhasesBasedOnCurrentHookPhase = getHookPhasesBasedOnCurrentHookPhase(currentHookPhaseForExecution,
                hook.getPhases());
            if (alreadyExecutedHooks.containsKey(hook.getName())) {
                alreadyExecutedHooks.get(hook.getName())
                    .addAll(hookPhasesBasedOnCurrentHookPhase);
                return;
            }
            alreadyExecutedHooks.put(hook.getName(), hookPhasesBasedOnCurrentHookPhase);
        }

        private List<String> getHookPhasesBasedOnCurrentHookPhase(HookPhase currentHookPhaseForExecution, List<String> hookPhases) {
            return hookPhases.stream()
                .filter(phase -> HookPhase.fromString(phase) == currentHookPhaseForExecution)
                .collect(Collectors.toList());
        }
    }

    public class HooksExecutor {

        private DelegateExecution context;
        private HooksExecutorFactory hooksExecutorFactory = new HooksExecutorFactory();
        private StepLogger stepLogger;
        private FlowableFacade flowableFacade;

        public HooksExecutor(DelegateExecution context, FlowableFacade flowableFacade, StepLogger stepLogger) {
            this.context = context;
            this.stepLogger = stepLogger;
            this.flowableFacade = flowableFacade;
        }

        public void executeHooks(HookPhase currentHookPhase, List<Hook> hooksToExecute) {
            executeHooks(context, currentHookPhase, hooksToExecute);
            reportHooksExecution(hooksToExecute);
        }

        private void executeHooks(DelegateExecution context, HookPhase currentHookPhase, List<Hook> hooksToExecute) {
            hooksToExecute.stream()
                .forEach(hook -> executeHook(currentHookPhase, hook));

        }

        private void executeHook(HookPhase currentHookPhase, Hook hookToExecute) {
            HookExecutor hookExecutor = hooksExecutorFactory.getHookExecutor(context, this.flowableFacade, hookToExecute.getType());
            HookExecution hookExecution = new HookExecution(currentHookPhase, hookToExecute, getOnCompleteHookMessageName());
            hookExecutor.executeHook(hookExecution);
        }

        private void reportHooksExecution(List<Hook> hooksForExecution) {
            hooksForExecution.forEach(this::reportHookExecution);
            StepsUtil.setHooksForExecution(context, hooksForExecution);
        }

        private void reportHookExecution(Hook hook) {
            stepLogger.info(Messages.EXECUTING_HOOK_0, hook.getName());
        }

    }

}
