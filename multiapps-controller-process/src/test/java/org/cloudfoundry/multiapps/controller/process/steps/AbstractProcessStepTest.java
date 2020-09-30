package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.core.model.ErrorType;
import org.cloudfoundry.multiapps.controller.process.MonitoringException;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

class AbstractProcessStepTest extends SyncFlowableStepTest<AbstractProcessStepTest.MockStep> {

    private static final String PROCESS_ID = "1234";

    public static Stream<Arguments> testExecute() {
        return Stream.of(
// @formatter:off
            Arguments.of(new UnsupportedOperationException(), ErrorType.UNKNOWN_ERROR),
            Arguments.of(new Exception(), ErrorType.UNKNOWN_ERROR),
            Arguments.of(new MonitoringException("Process \"4321\" failed, because of an infrastructure issue!"), ErrorType.UNKNOWN_ERROR),
            Arguments.of(new ContentException("There's something wrong with your MTA!"), ErrorType.CONTENT_ERROR)
// @formatter:on
        );
    }

    @BeforeEach
    public void setUp() {
        Mockito.when(execution.getProcessInstanceId())
               .thenReturn(PROCESS_ID);
    }

    @ParameterizedTest
    @MethodSource
    void testExecute(Exception exceptionToSimulate, ErrorType expectedErrorType) {
        step.exceptionSupplier = () -> exceptionToSimulate;
        try {
            step.execute(execution);
            fail();
        } catch (Exception e) {
            assertEquals(expectedErrorType, context.getVariable(Variables.ERROR_TYPE));
        }
    }

    public static class MockStep extends SyncFlowableStep {

        private Supplier<Exception> exceptionSupplier;

        @Override
        protected StepPhase executeStep(ProcessContext context) throws Exception {
            throw exceptionSupplier.get();
        }

        @Override
        protected String getStepErrorMessage(ProcessContext context) {
            return "mock error";
        }

    }

    @Override
    protected MockStep createStep() {
        return new MockStep();
    }

}