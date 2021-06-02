package org.cloudfoundry.multiapps.controller.process.steps;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import org.cloudfoundry.multiapps.controller.process.util.HooksCalculator;
import org.cloudfoundry.multiapps.controller.process.util.HooksExecutor;
import org.cloudfoundry.multiapps.controller.process.util.ModuleDeterminer;
import org.cloudfoundry.multiapps.controller.process.util.ProcessTypeParser;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.Hook;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

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
        Mockito.when(moduleDeterminer.determineModuleToDeploy())
               .thenReturn(moduleToDeploy);
        List<Hook> hooksForExecution = List.of(createHook("test-hook"));
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
        public Duration getTimeout(ProcessContext context) {
            return Duration.ofSeconds(1);
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
