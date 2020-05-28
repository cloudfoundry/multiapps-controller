package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import javax.inject.Inject;

import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.EnvMtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.MtaMetadataParser;
import com.sap.cloud.lm.sl.cf.process.util.HooksCalculator;
import com.sap.cloud.lm.sl.cf.process.util.HooksExecutor;
import com.sap.cloud.lm.sl.cf.process.util.HooksPhaseBuilder;
import com.sap.cloud.lm.sl.cf.process.util.HooksPhaseGetter;
import com.sap.cloud.lm.sl.cf.process.util.ImmutableHooksCalculator;
import com.sap.cloud.lm.sl.cf.process.util.ImmutableModuleDeterminer;
import com.sap.cloud.lm.sl.cf.process.util.ModuleDeterminer;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.mta.model.Hook;
import com.sap.cloud.lm.sl.mta.model.Module;

public abstract class SyncFlowableStepWithHooks extends SyncFlowableStep {

    @Inject
    private MtaMetadataParser mtaMetadataParser;
    @Inject
    private EnvMtaMetadataParser envMtaMetadataParser;
    @Inject
    private HooksPhaseGetter hooksPhaseGetter;
    @Inject
    protected HooksPhaseBuilder hooksPhaseBuilder;

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        ModuleDeterminer moduleDeterminer = getModuleDeterminer(context);
        StepPhase currentStepPhase = context.getVariable(Variables.STEP_PHASE);
        Module moduleToDeploy = moduleDeterminer.determineModuleToDeploy(context);
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
                                        .envMtaMetadataParser(envMtaMetadataParser)
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
