package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.process.util.HooksCalculator;
import com.sap.cloud.lm.sl.cf.process.util.HooksExecutor;
import com.sap.cloud.lm.sl.cf.process.util.ModuleDeterminer;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.mta.model.Hook;
import com.sap.cloud.lm.sl.mta.model.Module;

class SyncFlowableStepWithHooksStepTest extends SyncFlowableStepTest<SyncFlowableStepWithHooks> {

    @Mock
    private ModuleDeterminer moduleDeterminer;
    @Mock
    private HooksCalculator hooksCalculator;
    @Mock
    private HooksExecutor hooksExecutor;

    @Test
    void testExecuteStepPhaseWithHooksBefore() {
        Module moduleToDeploy = createModule("test-module");
        Mockito.when(moduleDeterminer.determineModuleToDeploy(context))
               .thenReturn(moduleToDeploy);
        List<Hook> hooksForExecution = Collections.singletonList(createHook("test-hook"));
        Mockito.when(hooksExecutor.executeBeforeStepHooks(hooksCalculator, moduleToDeploy, context.getVariable(Variables.STEP_PHASE)))
               .thenReturn(hooksForExecution);
        Assertions.assertEquals(StepPhase.EXECUTE, step.executeStep(context));
    }

    @Test
    void testExecuteStepPhaseWithHooksAfter() {
        Module moduleToDeploy = createModule("test-module");
        Mockito.when(moduleDeterminer.determineModuleToDeploy(context))
               .thenReturn(moduleToDeploy);
        List<Hook> hooksForExecution = Collections.singletonList(createHook("test-hook"));
        Mockito.when(hooksExecutor.executeAfterStepHooks(hooksCalculator, moduleToDeploy, context.getVariable(Variables.STEP_PHASE)))
               .thenReturn(hooksForExecution);
        Assertions.assertEquals(StepPhase.DONE, step.executeStep(context));
    }

    private Module createModule(String moduleName) {
        return Module.createV3()
                     .setName(moduleName);
    }

    private Hook createHook(String name) {
        return Hook.createV3()
                   .setName(name);
    }

    @Override
    protected SyncFlowableStepWithHooks createStep() {
        return new SyncFlowableStepWithHooksMock();
    }

    private class SyncFlowableStepWithHooksMock extends SyncFlowableStepWithHooks {

        @Override
        protected ModuleDeterminer getModuleDeterminer(ProcessContext context) {
            return moduleDeterminer;
        }

        @Override
        protected HooksCalculator getHooksCalculator(ProcessContext context) {
            return hooksCalculator;
        }

        @Override
        protected String getStepErrorMessage(ProcessContext context) {
            return "error occurred";
        }

        @Override
        protected StepPhase executeStepInternal(ProcessContext context) {
            return StepPhase.DONE;
        }
    }
}
