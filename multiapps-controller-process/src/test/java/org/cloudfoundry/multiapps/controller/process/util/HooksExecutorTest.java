package org.cloudfoundry.multiapps.controller.process.util;

import static org.mockito.ArgumentMatchers.any;

import java.util.Collections;
import java.util.List;

import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.process.steps.StepPhase;
import org.cloudfoundry.multiapps.mta.model.Hook;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class HooksExecutorTest {

    @Mock
    private HooksCalculator hooksCalculator;
    @Mock
    private ProcessTypeParser processTypeParser;

    HooksExecutorTest() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void executeBeforeStepHooksWhenPhaseIsNotBefore() {
        Module moduleToDeploy = createModule("test-module");
        HooksExecutor hooksExecutor = new HooksExecutor(hooksCalculator, moduleToDeploy);
        List<Hook> hooksForExecution = hooksExecutor.executeBeforeStepHooks(StepPhase.DONE);
        Assertions.assertTrue(hooksForExecution.isEmpty());
    }

    @Test
    void executeBeforeStepHooks() {
        Module moduleToDeploy = createModule("test-module");
        Mockito.when(hooksCalculator.isInPreExecuteStepPhase(StepPhase.EXECUTE))
               .thenReturn(true);
        Mockito.when(processTypeParser.getProcessType(any()))
               .thenReturn(ProcessType.BLUE_GREEN_DEPLOY);
        List<Hook> expectedHooksForExecution = List.of(createHook("test-hook", Collections.emptyList()));
        Mockito.when(hooksCalculator.calculateHooksForExecution(moduleToDeploy, StepPhase.EXECUTE))
               .thenReturn(expectedHooksForExecution);
        HooksExecutor hooksExecutor = new HooksExecutor(hooksCalculator, moduleToDeploy);
        List<Hook> hooksForExecution = hooksExecutor.executeBeforeStepHooks(StepPhase.EXECUTE);
        Assertions.assertEquals(expectedHooksForExecution, hooksForExecution);
    }

    @Test
    void executeAfterStepHooksWhenPhaseIsNotAfter() {
        Module moduleToDeploy = createModule("test-module");
        Mockito.when(processTypeParser.getProcessType(any()))
               .thenReturn(ProcessType.DEPLOY);
        HooksExecutor hooksExecutor = new HooksExecutor(hooksCalculator, moduleToDeploy);
        List<Hook> hooksForExecution = hooksExecutor.executeAfterStepHooks(StepPhase.EXECUTE);
        Assertions.assertTrue(hooksForExecution.isEmpty());
    }

    @Test
    void executeAfterStepHooks() {
        Module moduleToDeploy = createModule("test-module");
        Mockito.when(hooksCalculator.isInPostExecuteStepPhase(StepPhase.DONE))
               .thenReturn(true);
        List<Hook> expectedHooksForExecution = List.of(createHook("test-hook", Collections.emptyList()));
        Mockito.when(processTypeParser.getProcessType(any()))
               .thenReturn(ProcessType.DEPLOY);
        Mockito.when(hooksCalculator.calculateHooksForExecution(moduleToDeploy, StepPhase.DONE))
               .thenReturn(expectedHooksForExecution);
        HooksExecutor hooksExecutor = new HooksExecutor(hooksCalculator, moduleToDeploy);
        List<Hook> hooksForExecution = hooksExecutor.executeAfterStepHooks(StepPhase.DONE);
        Assertions.assertEquals(expectedHooksForExecution, hooksForExecution);
    }

    @Test
    void executeBeforeStepHooksWhenModuleToDeployIsNull() {
        Mockito.when(processTypeParser.getProcessType(any()))
               .thenReturn(ProcessType.DEPLOY);
        HooksExecutor hooksExecutor = new HooksExecutor(hooksCalculator, null);
        Mockito.when(hooksCalculator.isInPreExecuteStepPhase(StepPhase.EXECUTE))
               .thenReturn(true);
        List<Hook> hooksForExecution = hooksExecutor.executeBeforeStepHooks(StepPhase.EXECUTE);
        Assertions.assertTrue(hooksForExecution.isEmpty());
    }

    private Hook createHook(String name, List<String> phases) {
        return Hook.createV3()
                   .setName(name)
                   .setPhases(phases);
    }

    private Module createModule(String moduleName) {
        return Module.createV3()
                     .setName(moduleName);
    }
}
