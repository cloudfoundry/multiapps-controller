package com.sap.cloud.lm.sl.cf.process.util;

import java.util.Collections;
import java.util.List;

import javax.inject.Named;

import com.sap.cloud.lm.sl.cf.process.steps.StepPhase;
import com.sap.cloud.lm.sl.mta.model.Hook;
import com.sap.cloud.lm.sl.mta.model.Module;

@Named
public class HooksExecutor {

    public List<Hook> executeBeforeStepHooks(HooksCalculator hooksCalculator, Module moduleToDeploy, StepPhase currentStepPhase) {
        if (!hooksCalculator.isInPreExecuteStepPhase(currentStepPhase)) {
            return Collections.emptyList();
        }
        return executeHooks(hooksCalculator, currentStepPhase, moduleToDeploy);
    }

    public List<Hook> executeAfterStepHooks(HooksCalculator hooksCalculator, Module moduleToDeploy, StepPhase currentStepPhase) {
        if (!hooksCalculator.isInPostExecuteStepPhase(currentStepPhase)) {
            return Collections.emptyList();
        }
        return executeHooks(hooksCalculator, currentStepPhase, moduleToDeploy);
    }

    private List<Hook> executeHooks(HooksCalculator hooksCalculator, StepPhase stepPhase, Module moduleToDeploy) {
        if (moduleToDeploy == null) {
            return Collections.emptyList();
        }
        List<Hook> hooksForExecution = hooksCalculator.calculateHooksForExecution(moduleToDeploy, stepPhase);
        hooksCalculator.setHooksForExecution(hooksForExecution);
        return hooksForExecution;
    }

}
