package org.cloudfoundry.multiapps.controller.process.steps;

import java.time.Duration;
import java.util.List;

import org.cloudfoundry.multiapps.controller.core.cf.metadata.processor.MtaMetadataParser;
import org.cloudfoundry.multiapps.controller.process.util.HooksCalculator;
import org.cloudfoundry.multiapps.controller.process.util.HooksExecutor;
import org.cloudfoundry.multiapps.controller.process.util.HooksPhaseBuilder;
import org.cloudfoundry.multiapps.controller.process.util.HooksPhaseGetter;
import org.cloudfoundry.multiapps.controller.process.util.ImmutableHooksCalculator;
import org.cloudfoundry.multiapps.controller.process.util.ImmutableModuleDeterminer;
import org.cloudfoundry.multiapps.controller.process.util.ModuleDeterminer;
import org.cloudfoundry.multiapps.controller.process.util.TimeoutType;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.Hook;
import org.cloudfoundry.multiapps.mta.model.Module;

import jakarta.inject.Inject;

public abstract class TimeoutAsyncFlowableStepWithHooks extends TimeoutAsyncFlowableStep {

    @Inject
    private MtaMetadataParser mtaMetadataParser;
    @Inject
    private HooksPhaseGetter hooksPhaseGetter;
    @Inject
    protected HooksPhaseBuilder hooksPhaseBuilder;

    @Override
    public StepPhase executeAsyncStep(ProcessContext context) {
        StepPhase currentStepPhase = context.getVariable(Variables.STEP_PHASE);
        List<Hook> executedHooks = executeHooksForExecution(context, currentStepPhase);
        if (!executedHooks.isEmpty()) {
            return currentStepPhase;
        }
        return executePollingStep(context);
    }

    private List<Hook> executeHooksForExecution(ProcessContext context, StepPhase currentStepPhase) {
        HooksExecutor hooksExecutor = getHooksExecutor(context);
        return hooksExecutor.executeBeforeStepHooks(currentStepPhase);
    }

    private HooksExecutor getHooksExecutor(ProcessContext context) {
        ModuleDeterminer moduleDeterminer = getModuleDeterminer(context);
        Module moduleToDeploy = moduleDeterminer.determineModuleToDeploy();
        HooksCalculator hooksCalculator = getHooksCalculator(context);
        return getHooksDeterminer(hooksCalculator, moduleToDeploy, context);
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

    protected HooksExecutor getHooksDeterminer(HooksCalculator hooksCalculator, Module moduleToDeploy, ProcessContext context) {
        return new HooksExecutor(hooksCalculator, moduleToDeploy, context);
    }

    protected abstract StepPhase executePollingStep(ProcessContext context);

    @Override
    public Duration getTimeout(ProcessContext context) {
        StepPhase currentStepPhase = context.getVariable(Variables.STEP_PHASE);
        if (!getHooksExecutor(context).determineBeforeStepHooks(currentStepPhase)
                                      .isEmpty()) {
            return calculateTimeout(context, TimeoutType.TASK);
        }
        return null;
    }
}