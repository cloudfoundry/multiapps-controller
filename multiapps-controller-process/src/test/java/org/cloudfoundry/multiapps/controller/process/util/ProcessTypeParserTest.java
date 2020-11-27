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

public class ProcessTypeParserTest {

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

    static Stream<Arguments> testGetProcessType() {
        return Stream.of(Arguments.of(Constants.DEPLOY_SERVICE_ID, true, ProcessType.DEPLOY, false),
                         Arguments.of(Constants.BLUE_GREEN_DEPLOY_SERVICE_ID, true, ProcessType.BLUE_GREEN_DEPLOY, false),
                         Arguments.of(Constants.UNDEPLOY_SERVICE_ID, true, ProcessType.UNDEPLOY, false),
                         Arguments.of(null, true, null, true), Arguments.of(null, false, null, false),
                         Arguments.of(INVALID_SERVICE_ID, true, null, true), Arguments.of(INVALID_SERVICE_ID, false, null, false));
    }

    @ParameterizedTest
    @MethodSource
    protected void testGetProcessType(String serviceId, boolean required, ProcessType expectedProcessType, boolean expectedException) {
        prepareExecution(serviceId);

        if (expectedException) {
            assertThrows(SLException.class, () -> parser.getProcessType(execution, required));
            return;
        }
        ProcessType processType = parser.getProcessType(execution, required);
        assertEquals(expectedProcessType, processType);
    }

    private void prepareExecution(String serviceId) {
        when(execution.getVariable(Variables.SERVICE_ID.getName())).thenReturn(serviceId);
    }
}
