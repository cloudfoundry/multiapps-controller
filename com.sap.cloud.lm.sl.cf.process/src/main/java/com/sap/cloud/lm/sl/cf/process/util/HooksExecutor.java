package com.sap.cloud.lm.sl.cf.process.util;

import java.util.Collections;
import java.util.List;

import org.cloudfoundry.multiapps.mta.model.Hook;
import org.cloudfoundry.multiapps.mta.model.Module;

import com.sap.cloud.lm.sl.cf.process.steps.StepPhase;

public class HooksExecutor {

    private final HooksCalculator hooksCalculator;
    private final Module moduleToDeploy;

    public HooksExecutor(HooksCalculator hooksCalculator, Module moduleToDeploy) {
        this.hooksCalculator = hooksCalculator;
        this.moduleToDeploy = moduleToDeploy;
    }

    public List<Hook> executeBeforeStepHooks(StepPhase currentStepPhase) {
        if (!hooksCalculator.isInPreExecuteStepPhase(currentStepPhase)) {
            return Collections.emptyList();
        }
        return executeHooks(currentStepPhase);
    }

    public List<Hook> executeAfterStepHooks(StepPhase currentStepPhase) {
        if (!hooksCalculator.isInPostExecuteStepPhase(currentStepPhase)) {
            return Collections.emptyList();
        }
        return executeHooks(currentStepPhase);
    }

    private List<Hook> executeHooks(StepPhase stepPhase) {
        if (moduleToDeploy == null) {
            return Collections.emptyList();
        }
        List<Hook> hooksForExecution = hooksCalculator.calculateHooksForExecution(moduleToDeploy, stepPhase);
        hooksCalculator.setHooksForExecution(hooksForExecution);
        return hooksForExecution;
    }

}
