package org.cloudfoundry.multiapps.controller.process.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.process.steps.StepPhase;
import org.cloudfoundry.multiapps.mta.model.Hook;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class HooksExecutorTest {

    @Mock
    private HooksCalculator hooksCalculator;
    @Mock
    private ProcessTypeParser processTypeParser;
    @Mock
    private DelegateExecution delegateExecution;

    HooksExecutorTest() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void executeBeforeStepHooksWhenPhaseIsNotBefore() {
        Module moduleToDeploy = createModule("test-module");
        HooksExecutor hooksExecutor = new HooksExecutor(hooksCalculator, moduleToDeploy, delegateExecution);
        List<Hook> hooksForExecution = hooksExecutor.executeBeforeStepHooks(StepPhase.DONE);
        Assertions.assertTrue(hooksForExecution.isEmpty());
    }

    @Test
    void executeBeforeStepHooks() {
        Module moduleToDeploy = createModule("test-module");
        when(hooksCalculator.isInPreExecuteStepPhase(StepPhase.EXECUTE)).thenReturn(true);
        when(processTypeParser.getProcessType(any())).thenReturn(ProcessType.BLUE_GREEN_DEPLOY);
        List<Hook> expectedHooksForExecution = List.of(createHook("test-hook", Collections.emptyList()));
        when(hooksCalculator.calculateHooksForExecution(moduleToDeploy, StepPhase.EXECUTE)).thenReturn(ImmutableHooksWithPhases.builder()
                                                                                                                               .hooks(expectedHooksForExecution)
                                                                                                                               .hookPhases(Collections.emptyList())
                                                                                                                               .build());
        HooksExecutor hooksExecutor = new HooksExecutor(hooksCalculator, moduleToDeploy, delegateExecution);
        List<Hook> hooksForExecution = hooksExecutor.executeBeforeStepHooks(StepPhase.EXECUTE);
        Assertions.assertEquals(expectedHooksForExecution, hooksForExecution);
    }

    @Test
    void executeAfterStepHooksWhenPhaseIsNotAfter() {
        Module moduleToDeploy = createModule("test-module");
        when(processTypeParser.getProcessType(any())).thenReturn(ProcessType.DEPLOY);
        HooksExecutor hooksExecutor = new HooksExecutor(hooksCalculator, moduleToDeploy, delegateExecution);
        List<Hook> hooksForExecution = hooksExecutor.executeAfterStepHooks(StepPhase.EXECUTE);
        Assertions.assertTrue(hooksForExecution.isEmpty());
    }

    @Test
    void executeAfterStepHooks() {
        Module moduleToDeploy = createModule("test-module");
        when(hooksCalculator.isInPostExecuteStepPhase(StepPhase.DONE)).thenReturn(true);
        List<Hook> expectedHooksForExecution = List.of(createHook("test-hook", Collections.emptyList()));
        when(processTypeParser.getProcessType(any())).thenReturn(ProcessType.DEPLOY);
        when(hooksCalculator.calculateHooksForExecution(moduleToDeploy, StepPhase.DONE)).thenReturn(ImmutableHooksWithPhases.builder()
                                                                                                                            .hooks(expectedHooksForExecution)
                                                                                                                            .hookPhases(Collections.emptyList())
                                                                                                                            .build());
        HooksExecutor hooksExecutor = new HooksExecutor(hooksCalculator, moduleToDeploy, delegateExecution);
        List<Hook> hooksForExecution = hooksExecutor.executeAfterStepHooks(StepPhase.DONE);
        Assertions.assertEquals(expectedHooksForExecution, hooksForExecution);
    }

    @Test
    void executeBeforeStepHooksWhenModuleToDeployIsNull() {
        when(processTypeParser.getProcessType(any())).thenReturn(ProcessType.DEPLOY);
        HooksExecutor hooksExecutor = new HooksExecutor(hooksCalculator, null, delegateExecution);
        when(hooksCalculator.isInPreExecuteStepPhase(StepPhase.EXECUTE)).thenReturn(true);
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
