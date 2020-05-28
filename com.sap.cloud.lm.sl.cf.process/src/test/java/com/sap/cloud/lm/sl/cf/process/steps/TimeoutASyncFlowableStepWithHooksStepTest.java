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
import com.sap.cloud.lm.sl.cf.process.util.ProcessTypeParser;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.mta.model.Hook;
import com.sap.cloud.lm.sl.mta.model.Module;

class TimeoutASyncFlowableStepWithHooksStepTest extends SyncFlowableStepTest<TimeoutAsyncFlowableStepWithHooks> {

    @Mock
    private ModuleDeterminer moduleDeterminer;
    @Mock
    private HooksCalculator hooksCalculator;
    @Mock
    private HooksExecutor hooksExecutor;
    @Mock
    private ProcessTypeParser processTypeParser;

    @Test
    void testExecuteStepPhaseWithHooksBefore() {
        Module moduleToDeploy = createModule("test-module");
        Mockito.when(moduleDeterminer.determineModuleToDeploy(context))
               .thenReturn(moduleToDeploy);
        List<Hook> hooksForExecution = Collections.singletonList(createHook("test-hook"));
        Mockito.when(hooksExecutor.executeBeforeStepHooks(context.getVariable(Variables.STEP_PHASE)))
               .thenReturn(hooksForExecution);
        Assertions.assertEquals(StepPhase.EXECUTE, step.executeAsyncStep(context));
    }

    @Test
    void testExecuteStepPhaseWithHooks() {
        Assertions.assertEquals(StepPhase.POLL, step.executeAsyncStep(context));
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
    protected TimeoutAsyncFlowableStepWithHooks createStep() {
        return new TimeoutAsyncFlowableStepWithHookMock();
    }

    private class TimeoutAsyncFlowableStepWithHookMock extends TimeoutAsyncFlowableStepWithHooks {

        @Override
        protected ModuleDeterminer getModuleDeterminer(ProcessContext context) {
            return moduleDeterminer;
        }

        @Override
        protected HooksCalculator getHooksCalculator(ProcessContext context) {
            return hooksCalculator;
        }

        @Override
        protected StepPhase executePollingStep(ProcessContext context) {
            return StepPhase.POLL;
        }

        @Override
        protected String getStepErrorMessage(ProcessContext context) {
            return "error occurred";
        }

        @Override
        public Integer getTimeout(ProcessContext context) {
            return 1;
        }

        @Override
        protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
            return Collections.emptyList();
        }

        @Override
        protected HooksExecutor getHooksExecutor(HooksCalculator hooksCalculator, Module moduleToDeploy) {
            return hooksExecutor;
        }
    }
}
