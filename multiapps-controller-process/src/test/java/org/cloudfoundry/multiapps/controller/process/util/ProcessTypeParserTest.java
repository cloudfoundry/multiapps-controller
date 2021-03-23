package org.cloudfoundry.multiapps.controller.process.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ProcessTypeParserTest {

    private static final String INVALID_SERVICE_ID = "invalid-service-id";

    @Mock
    private DelegateExecution execution;

    private ProcessTypeParser parser;

    @BeforeEach
    protected void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        parser = new ProcessTypeParser();
    }

    static Stream<Arguments> testGetProcessTypeFromProcessVariable() {
        return Stream.of(Arguments.of(Constants.DEPLOY_SERVICE_ID, true, ProcessType.DEPLOY, false),
                         Arguments.of(Constants.BLUE_GREEN_DEPLOY_SERVICE_ID, true, ProcessType.BLUE_GREEN_DEPLOY, false),
                         Arguments.of(Constants.UNDEPLOY_SERVICE_ID, true, ProcessType.UNDEPLOY, false),
                         Arguments.of(null, true, null, true), Arguments.of(null, false, null, false),
                         Arguments.of(INVALID_SERVICE_ID, true, null, true), Arguments.of(INVALID_SERVICE_ID, false, null, false));
    }

    @ParameterizedTest
    @MethodSource
    void testGetProcessTypeFromProcessVariable(String serviceId, boolean required, ProcessType expectedProcessType,
                                               boolean expectedException) {
        prepareProcessVariable(serviceId);

        if (expectedException) {
            assertThrows(SLException.class, () -> parser.getProcessTypeFromProcessVariable(execution, required));
            return;
        }
        ProcessType processType = parser.getProcessTypeFromProcessVariable(execution, required);
        assertEquals(expectedProcessType, processType);
    }

    private void prepareProcessVariable(String serviceId) {
        when(execution.getVariable(Variables.SERVICE_ID.getName())).thenReturn(serviceId);
    }

    static Stream<Arguments> testGetProcessTypeFromExecution() {
        // Process Definition Id is String of {processDefinitionKey}:{processDefinitionVersion}:{generated-id}
        return Stream.of(Arguments.of(Constants.DEPLOY_SERVICE_ID + ":1:1234", ProcessType.DEPLOY, false),
                         Arguments.of(Constants.BLUE_GREEN_DEPLOY_SERVICE_ID + ":10:4667", ProcessType.BLUE_GREEN_DEPLOY, false),
                         Arguments.of(Constants.UNDEPLOY_SERVICE_ID + ":2:44", ProcessType.UNDEPLOY, false),
                         Arguments.of(INVALID_SERVICE_ID, null, true), Arguments.of(Constants.BLUE_GREEN_DEPLOY_SERVICE_ID, null, true));

    }

    @ParameterizedTest
    @MethodSource
    void testGetProcessTypeFromExecution(String processDefinitionId, ProcessType expectedProcessType, boolean expectedException) {
        prepareProcessDefinitionId(processDefinitionId);

        if (expectedException) {
            assertThrows(SLException.class, () -> parser.getProcessTypeFromExecution(execution));
            return;
        }
        ProcessType processType = parser.getProcessTypeFromExecution(execution);
        assertEquals(expectedProcessType, processType);
    }

    private void prepareProcessDefinitionId(String processDefinitionId) {
        when(execution.getProcessDefinitionId()).thenReturn(processDefinitionId);
    }

}
