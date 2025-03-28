package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.process.util.BlueGreenVariablesSetter;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;

class PrepareModulesDeploymentStepTest extends SyncFlowableStepTest<PrepareModulesDeploymentStep> {

    @Mock
    private BlueGreenVariablesSetter blueGreenVariablesSetter;

    public static Stream<Arguments> testExecute() {
        return Stream.of(
            // @formatter:off
            Arguments.of(1, ProcessType.DEPLOY), 
            Arguments.of(2, ProcessType.DEPLOY), 
            Arguments.of(3, ProcessType.DEPLOY), 
            Arguments.of(4, ProcessType.BLUE_GREEN_DEPLOY), 
            Arguments.of(5, ProcessType.UNDEPLOY)
         // @formatter:on    
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExecute(int count, ProcessType processType) {
        initializeParameters(count, processType);
        step.execute(execution);

        assertStepFinishedSuccessfully();

        assertEquals((Integer) count, context.getVariable(Variables.MODULES_COUNT));
        assertEquals((Integer) 0, context.getVariable(Variables.MODULES_INDEX));
        assertEquals(Variables.MODULES_INDEX.getName(), context.getVariable(Variables.INDEX_VARIABLE_NAME));
        assertTrue(context.getVariable(Variables.REBUILD_APP_ENV));
        assertTrue(context.getVariable(Variables.SHOULD_UPLOAD_APPLICATION_CONTENT));
        assertTrue(context.getVariable(Variables.EXECUTE_ONE_OFF_TASKS));
        verify(blueGreenVariablesSetter).set(any());
    }

    private void initializeParameters(int count, ProcessType processType) {
        prepareContext(count);
        Mockito.when(configuration.getStepPollingIntervalInSeconds())
               .thenReturn(ApplicationConfiguration.DEFAULT_STEP_POLLING_INTERVAL_IN_SECONDS);
    }

    private void prepareContext(int count) {
        context.setVariable(Variables.ALL_MODULES_TO_DEPLOY, getDummyModules(count));
    }

    private List<Module> getDummyModules(int count) {
        List<Module> modules = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            modules.add(Module.createV2());
        }
        return modules;
    }

    @Override
    protected PrepareModulesDeploymentStep createStep() {
        return new PrepareModulesDeploymentStep(blueGreenVariablesSetter);
    }

}
