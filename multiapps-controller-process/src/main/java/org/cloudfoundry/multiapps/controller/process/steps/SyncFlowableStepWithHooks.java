package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;

import javax.inject.Inject;

import org.cloudfoundry.multiapps.controller.core.cf.metadata.processor.MtaMetadataParser;
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
        HooksExecutor hooksExecutor = getHooksExecutor(hooksCalculator, moduleToDeploy);
        List<Hook> executedHooks = hooksExecutor.executeBeforeStepHooks(currentStepPhase);
        if (!executedHooks.isEmpty()) {
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

    protected HooksExecutor getHooksExecutor(HooksCalculator hooksCalculator, Module moduleToDeploy) {
        return new HooksExecutor(hooksCalculator, moduleToDeploy);
    }

    protected abstract StepPhase executeStepInternal(ProcessContext context);

}
