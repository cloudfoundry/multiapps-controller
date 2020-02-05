package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.ListUtils;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.EnvMtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.MtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.model.HookPhase;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Hook;
import com.sap.cloud.lm.sl.mta.model.Module;

public abstract class SyncFlowableStepWithHooks extends SyncFlowableStep {

    @Inject
    private MtaMetadataParser mtaMetadataParser;

    @Inject
    private EnvMtaMetadataParser envMtaMetadataParser;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws Exception {
        Module moduleToDeploy = determineModuleToDeploy(execution.getContext());

        if (moduleToDeploy == null) {
            return executeStepInternal(execution);
        }

        StepPhase currentStepPhase = StepsUtil.getStepPhase(execution.getContext());
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
        HookPhase currentHookPhaseForExecution = determineHookPhaseForCurrentStepPhase(context, currentStepPhase);
        List<Hook> hooksForCurrentPhase = getHooksForCurrentPhase(context, moduleToDeploy, currentHookPhaseForExecution);
        setHooksForExecution(context, hooksForCurrentPhase);

        return hooksForCurrentPhase;
    }

    private Module determineModuleToDeploy(DelegateExecution context) {
        Module moduleToDeploy = StepsUtil.getModuleToDeploy(context);

        return moduleToDeploy != null ? moduleToDeploy : determineModuleFromDescriptor(context);
    }

    private Module determineModuleFromDescriptor(DelegateExecution context) {
        DeploymentDescriptor deploymentDescriptor = StepsUtil.getCompleteDeploymentDescriptor(context);
        if (deploymentDescriptor == null) {
            // This will be the case only when the process is undeploy.
            return null;
        }

        CloudApplicationExtended cloudApplication = StepsUtil.getApp(context);
        if (cloudApplication.getModuleName() == null) {
            // This case handles the deletion of old applications when the process is blue-green deployment. Here the application is taken
            // from the
            // CloudController and thus we do not have moduleName in it.
            return determineModuleFromAppName(deploymentDescriptor, cloudApplication);
        }

        HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context);
        return findModuleByNameFromDeploymentDescriptor(handlerFactory, deploymentDescriptor, cloudApplication.getModuleName());
    }

    private Module determineModuleFromAppName(DeploymentDescriptor deploymentDescriptor, CloudApplicationExtended cloudApplication) {
        String moduleNameFromApplicationEnvironment = getModuleName(cloudApplication);
        if (moduleNameFromApplicationEnvironment == null) {
            return null;
        }
        return deploymentDescriptor.getModules()
                                   .stream()
                                   .filter(module -> moduleNameFromApplicationEnvironment.equals(module.getName()))
                                   .findFirst()
                                   .orElse(null);
    }

    protected String getModuleName(CloudApplicationExtended cloudApplication) {
        return getDeployedMtaApplication(cloudApplication).getModuleName();
    }

    private DeployedMtaApplication getDeployedMtaApplication(CloudApplication app) {
        if (app.getV3Metadata() == null) {
            return envMtaMetadataParser.parseDeployedMtaApplication(app);
        }
        return mtaMetadataParser.parseDeployedMtaApplication(app);
    }

    private Module findModuleByNameFromDeploymentDescriptor(HandlerFactory handlerFactory, DeploymentDescriptor deploymentDescriptor,
                                                            String moduleName) {
        return handlerFactory.getDescriptorHandler()
                             .findModule(deploymentDescriptor, moduleName);
    }

    private List<Hook> getHooksForCurrentPhase(DelegateExecution context, Module moduleToDeploy, HookPhase currentHookPhaseForExecution) {
        return getModuleHooksAggregator(context, moduleToDeploy).aggregateHooks(currentHookPhaseForExecution);
    }

    protected ModuleHooksAggregator getModuleHooksAggregator(DelegateExecution context, Module moduleToDeploy) {
        return new ModuleHooksAggregator(context, moduleToDeploy);
    }

    private void setHooksForExecution(DelegateExecution context, List<Hook> hooksForExecution) {
        StepsUtil.setHooksForExecution(context, hooksForExecution);
    }

    private boolean isInPreExecuteStepPhase(StepPhase currentStepPhase) {
        return currentStepPhase == StepPhase.EXECUTE || currentStepPhase == StepPhase.RETRY;
    }

    private boolean isInPostExecuteStepPhase(StepPhase currentStepPhase) {
        return currentStepPhase == StepPhase.DONE;
    }

    protected HookPhase getHookPhaseBeforeStep(DelegateExecution context) {
        return HookPhase.NONE;
    }

    protected HookPhase getHookPhaseAfterStep(DelegateExecution context) {
        return HookPhase.NONE;
    }

    private HookPhase determineHookPhaseForCurrentStepPhase(DelegateExecution context, StepPhase currentStepPhase) {
        if (isInPreExecuteStepPhase(currentStepPhase)) {
            return getHookPhaseBeforeStep(context);
        }
        if (isInPostExecuteStepPhase(currentStepPhase)) {
            return getHookPhaseAfterStep(context);
        }
        return HookPhase.NONE;
    }

    protected abstract StepPhase executeStepInternal(ExecutionWrapper execution);

    static class ModuleHooksAggregator {

        private final DelegateExecution context;
        private final Module moduleToDeploy;

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
                                                                           .filter(hookToBeExecuted -> !getExecutedHookPhasesForHook(alreadyExecutedHooks,
                                                                                                                                     hookToBeExecuted.getName()).contains(hookPhaseForCurrentStepPhase))
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

}
