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
import com.sap.cloud.lm.sl.cf.process.exception.MonitoringException;
import com.sap.cloud.lm.sl.common.ContentException;

@RunWith(Parameterized.class)
public class AbstractProcessStepTest extends SyncFlowableStepTest<AbstractProcessStepTest.MockStep> {

    private static final String PROCESS_ID = "1234";
    private Exception exceptionToSimulate;
    private ErrorType expectedErrorType;
    public AbstractProcessStepTest(Exception exceptionToSimulate, ErrorType expectedErrorType) {
        this.exceptionToSimulate = exceptionToSimulate;
        this.expectedErrorType = expectedErrorType;
    }

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

    @Before
    public void setUp() {
        Mockito.when(context.getProcessInstanceId())
               .thenReturn(PROCESS_ID);
    }

    @Test
    public void testExecute() throws Exception {
        step.exceptionSupplier = () -> exceptionToSimulate;
        try {
            step.execute(context);
            fail();
        } catch (Exception e) {
            Mockito.verify(context)
                   .setVariable(Constants.VAR_ERROR_TYPE, expectedErrorType.toString());
        }
    }

    @Override
    protected MockStep createStep() {
        return new MockStep();
    }

    public static class MockStep extends SyncFlowableStep {

        private Supplier<Exception> exceptionSupplier;

        @Override
        protected StepPhase executeStep(ExecutionWrapper execution) throws Exception {
            throw exceptionSupplier.get();
        }

    }

}