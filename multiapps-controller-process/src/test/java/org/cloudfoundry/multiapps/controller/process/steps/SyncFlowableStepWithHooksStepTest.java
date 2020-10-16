package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;

import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
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

class SyncFlowableStepWithHooksStepTest extends SyncFlowableStepTest<SyncFlowableStepWithHooks> {

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
        Mockito.when(processTypeParser.getProcessType(context.getExecution()))
               .thenReturn(ProcessType.DEPLOY);
        Module moduleToDeploy = createModule("test-module");
        Mockito.when(moduleDeterminer.determineModuleToDeploy())
               .thenReturn(moduleToDeploy);
        List<Hook> hooksForExecution = List.of(createHook("test-hook"));
        Mockito.when(hooksExecutor.executeBeforeStepHooks(StepPhase.EXECUTE))
               .thenReturn(hooksForExecution);
        Assertions.assertEquals(StepPhase.EXECUTE, step.executeStep(context));
    }

    @Test
    void testExecuteStepPhaseWithHooksAfter() {
        Module moduleToDeploy = createModule("test-module");
        Mockito.when(moduleDeterminer.determineModuleToDeploy())
               .thenReturn(moduleToDeploy);
        List<Hook> hooksForExecution = List.of(createHook("test-hook"));
        Mockito.when(hooksExecutor.executeAfterStepHooks(context.getVariable(Variables.STEP_PHASE)))
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

        @Override
        protected HooksExecutor getHooksExecutor(HooksCalculator hooksCalculator, Module moduleToDeploy) {
            return hooksExecutor;
        }
    }
}
