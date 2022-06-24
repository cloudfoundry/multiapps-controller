package org.cloudfoundry.multiapps.controller.process.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class BlueGreenVariablesSetterTest {

    @Mock
    private DeploymentTypeDeterminer deploymentTypeDeterminer;
    @Mock
    private ProcessContext processContext;

    private BlueGreenVariablesSetter blueGreenVariablesSetter;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        blueGreenVariablesSetter = new BlueGreenVariablesSetter(deploymentTypeDeterminer);
    }

    static Stream<Arguments> testSet() {
        return Stream.of(Arguments.of(ProcessType.DEPLOY, false), Arguments.of(ProcessType.BLUE_GREEN_DEPLOY, true),
                         Arguments.of(ProcessType.UNDEPLOY, false));
    }

    @ParameterizedTest
    @MethodSource
    void testSet(ProcessType processType, boolean expectedBlueGreenUpdate) {
        prepareProcessTypeParser(processType);

        blueGreenVariablesSetter.set(processContext);

        verify(processContext).setVariable(Variables.SKIP_UPDATE_CONFIGURATION_ENTRIES, expectedBlueGreenUpdate);
        verify(processContext).setVariable(Variables.SKIP_MANAGE_SERVICE_BROKER, expectedBlueGreenUpdate);
        verify(processContext).setVariable(Variables.USE_IDLE_URIS, expectedBlueGreenUpdate);
    }

    private void prepareProcessTypeParser(ProcessType processType) {
        when(deploymentTypeDeterminer.determineDeploymentType(any())).thenReturn(processType);
    }

}
