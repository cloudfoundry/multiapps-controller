package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.core.model.ErrorType;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.MonitoringException;
import com.sap.cloud.lm.sl.common.ContentException;

@RunWith(Parameterized.class)
public class AbstractProcessStepTest extends SyncFlowableStepTest<AbstractProcessStepTest.MockStep> {

    private static final String PROCESS_ID = "1234";

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            { new UnsupportedOperationException(), ErrorType.UNKNOWN_ERROR, },
            { new Exception(), ErrorType.UNKNOWN_ERROR, },
            { new MonitoringException("Process \"4321\" failed, because of an infrastructure issue!"), ErrorType.UNKNOWN_ERROR, },
            { new ContentException("There's something wrong with your MTA!"), ErrorType.CONTENT_ERROR, },
// @formatter:on
        });
    }

    private final Exception exceptionToSimulate;
    private final ErrorType expectedErrorType;

    public AbstractProcessStepTest(Exception exceptionToSimulate, ErrorType expectedErrorType) {
        this.exceptionToSimulate = exceptionToSimulate;
        this.expectedErrorType = expectedErrorType;
    }

    @Before
    public void setUp() {
        Mockito.when(execution.getProcessInstanceId())
               .thenReturn(PROCESS_ID);
    }

    @Test
    public void testExecute() {
        step.exceptionSupplier = () -> exceptionToSimulate;
        try {
            step.execute(execution);
            fail();
        } catch (Exception e) {
            Mockito.verify(execution)
                   .setVariable(Constants.VAR_ERROR_TYPE, expectedErrorType.toString());
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