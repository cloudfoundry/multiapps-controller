package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.processor.MtaMetadataParser;
import org.cloudfoundry.multiapps.controller.core.model.HookPhase;
import org.cloudfoundry.multiapps.controller.process.util.HooksCalculator;
import org.cloudfoundry.multiapps.controller.process.util.HooksExecutor;
import org.cloudfoundry.multiapps.controller.process.util.HooksPhaseBuilder;
import org.cloudfoundry.multiapps.controller.process.util.HooksPhaseGetter;
import org.cloudfoundry.multiapps.controller.process.util.ImmutableHooksCalculator;
import org.cloudfoundry.multiapps.controller.process.util.ImmutableModuleDeterminer;
import org.cloudfoundry.multiapps.controller.process.util.ModuleDeterminer;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.Hook;
import org.cloudfoundry.multiapps.mta.model.Module;

public abstract class SyncFlowableStepWithHooks extends SyncFlowableStep {

    @Inject
    private MtaMetadataParser mtaMetadataParser;
    @Inject
    private HooksPhaseGetter hooksPhaseGetter;
    @Inject
    protected HooksPhaseBuilder hooksPhaseBuilder;

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        ModuleDeterminer moduleDeterminer = getModuleDeterminer(context);
        StepPhase currentStepPhase = context.getVariable(Variables.STEP_PHASE);
        Module moduleToDeploy = moduleDeterminer.determineModuleToDeploy();
        HooksCalculator hooksCalculator = getHooksCalculator(context);
        HooksExecutor hooksExecutor = getHooksExecutor(hooksCalculator, moduleToDeploy, context);
        List<Hook> executedBeforeStepHooks = hooksExecutor.executeBeforeStepHooks(currentStepPhase);
        if (!executedBeforeStepHooks.isEmpty()) {
            return currentStepPhase;
        }
        currentStepPhase = executeStepInternal(context);
        hooksExecutor.executeAfterStepHooks(currentStepPhase);
        return currentStepPhase;
    }

    protected ModuleDeterminer getModuleDeterminer(ProcessContext context) {
        return ImmutableModuleDeterminer.builder()
                                        .context(context)
                                        .mtaMetadataParser(mtaMetadataParser)
                                        .build();
    }

    protected HooksCalculator getHooksCalculator(ProcessContext context) {
        return ImmutableHooksCalculator.builder()
                                       .context(context)
                                       .hookPhasesBeforeStep(hooksPhaseGetter.getHookPhasesBeforeStop(this, context))
                                       .hookPhasesAfterStep(hooksPhaseGetter.getHookPhasesAfterStop(this, context))
                                       .build();
    }

    protected HooksExecutor getHooksExecutor(HooksCalculator hooksCalculator, Module moduleToDeploy, ProcessContext context) {
        return new HooksExecutor(hooksCalculator, moduleToDeploy, context);
    }

    protected abstract StepPhase executeStepInternal(ProcessContext context);

    private List<Hook> collectHooksWithPhase(List<HookPhase> hookPhasesForCurrentStepPhase, Module moduleToDeploy) {
        return getModuleHooks(moduleToDeploy).stream()
                                             .filter(hook -> shouldCollectHook(hook.getPhases(), hookPhasesForCurrentStepPhase))
                                             .collect(Collectors.toList());
    }

    private List<Hook> getModuleHooks(Module moduleToDeploy) {
        return moduleToDeploy.getMajorSchemaVersion() < 3 ? Collections.emptyList() : moduleToDeploy.getHooks();
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
}
