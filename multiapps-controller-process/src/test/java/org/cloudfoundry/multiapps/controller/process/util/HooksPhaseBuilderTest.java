package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;

import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.model.HookPhase;
import org.cloudfoundry.multiapps.controller.core.model.Phase;
import org.cloudfoundry.multiapps.controller.core.model.SubprocessPhase;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class HooksPhaseBuilderTest {

    private final ProcessContext context = createContext();
    @Mock
    private DeploymentTypeDeterminer deploymentTypeDeterminer;

    HooksPhaseBuilderTest() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void testBuildHookPhaseForDeployProcess() {
        Mockito.when(deploymentTypeDeterminer.determineDeploymentType(context))
               .thenReturn(ProcessType.DEPLOY);
        HooksPhaseBuilder hooksPhaseBuilder = new HooksPhaseBuilder(deploymentTypeDeterminer);
        List<HookPhase> hookPhases = hooksPhaseBuilder.buildHookPhases(List.of(HookPhase.BEFORE_STOP), context);
        Assertions.assertEquals(List.of(HookPhase.DEPLOY_APPLICATION_BEFORE_STOP), hookPhases);
    }

    @Test
    void testBuildHookPhaseForBlueGreenDeployProcessWithSubprocessPhaseBeforeApplicationStop() {
        Mockito.when(deploymentTypeDeterminer.determineDeploymentType(context))
               .thenReturn(ProcessType.BLUE_GREEN_DEPLOY);
        context.setVariable(Variables.SUBPROCESS_PHASE, SubprocessPhase.BEFORE_APPLICATION_STOP);
        HooksPhaseBuilder hooksPhaseBuilder = new HooksPhaseBuilder(deploymentTypeDeterminer);
        List<HookPhase> hookPhases = hooksPhaseBuilder.buildHookPhases(List.of(HookPhase.BEFORE_STOP), context);
        Assertions.assertEquals(List.of(HookPhase.BLUE_GREEN_APPLICATION_BEFORE_STOP_IDLE), hookPhases);
    }

    @Test
    void testBuildHookPhaseForBlueGreenDeployProcessWithSubprocessPhaseBeforeApplicationStart() {
        Mockito.when(deploymentTypeDeterminer.determineDeploymentType(context))
               .thenReturn(ProcessType.BLUE_GREEN_DEPLOY);
        context.setVariable(Variables.SUBPROCESS_PHASE, SubprocessPhase.BEFORE_APPLICATION_START);
        HooksPhaseBuilder hooksPhaseBuilder = new HooksPhaseBuilder(deploymentTypeDeterminer);
        List<HookPhase> hookPhases = hooksPhaseBuilder.buildHookPhases(List.of(HookPhase.BEFORE_START), context);
        Assertions.assertEquals(List.of(HookPhase.BLUE_GREEN_APPLICATION_BEFORE_START_IDLE), hookPhases);
    }

    @Test
    void testBuildHookPhaseForBlueGreenProcessWithPhaseUndeploy() {
        Mockito.when(deploymentTypeDeterminer.determineDeploymentType(context))
               .thenReturn(ProcessType.BLUE_GREEN_DEPLOY);
        context.setVariable(Variables.PHASE, Phase.UNDEPLOY);
        HooksPhaseBuilder hooksPhaseBuilder = new HooksPhaseBuilder(deploymentTypeDeterminer);
        List<HookPhase> hookPhases = hooksPhaseBuilder.buildHookPhases(List.of(HookPhase.AFTER_STOP), context);
        Assertions.assertEquals(List.of(HookPhase.BLUE_GREEN_APPLICATION_AFTER_STOP_LIVE), hookPhases);
    }

    @Test
    void testBuildHookPhaseForBlueGreenProcessWithPhaseAfterResume() {
        Mockito.when(deploymentTypeDeterminer.determineDeploymentType(context))
               .thenReturn(ProcessType.BLUE_GREEN_DEPLOY);
        context.setVariable(Variables.PHASE, Phase.AFTER_RESUME);
        HooksPhaseBuilder hooksPhaseBuilder = new HooksPhaseBuilder(deploymentTypeDeterminer);
        List<HookPhase> hookPhases = hooksPhaseBuilder.buildHookPhases(List.of(HookPhase.BEFORE_START), context);
        Assertions.assertEquals(List.of(HookPhase.BLUE_GREEN_APPLICATION_BEFORE_START_LIVE), hookPhases);
    }

    private ProcessContext createContext() {
        DelegateExecution delegateExecution = MockDelegateExecution.createSpyInstance();
        StepLogger stepLogger = Mockito.mock(StepLogger.class);
        CloudControllerClientProvider cloudControllerClientProvider = Mockito.mock(CloudControllerClientProvider.class);
        return new ProcessContext(delegateExecution, stepLogger, cloudControllerClientProvider);
    }
}
