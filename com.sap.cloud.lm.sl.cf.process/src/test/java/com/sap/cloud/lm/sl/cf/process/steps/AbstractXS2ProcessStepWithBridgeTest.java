package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.function.Supplier;

import org.activiti.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.model.ErrorType;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.exception.MonitoringException;
import com.sap.cloud.lm.sl.common.ContentException;

@RunWith(Parameterized.class)
public class AbstractXS2ProcessStepWithBridgeTest extends AbstractStepTest<AbstractXS2ProcessStepWithBridgeTest.MockStep> {

    private static final String PROCESS_ID = "1234";
    private static final String INDEX_VARIABLE_NAME = "indexVariable";

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

    private Exception exceptionToSimulate;
    private ErrorType expectedErrorType;

    public AbstractXS2ProcessStepWithBridgeTest(Exception exceptionToSimulate, ErrorType expectedErrorType) {
        this.exceptionToSimulate = exceptionToSimulate;
        this.expectedErrorType = expectedErrorType;
    }

    @Before
    public void setUp() {
        Mockito.when(context.getProcessInstanceId()).thenReturn(PROCESS_ID);
        context.setVariable(INDEX_VARIABLE_NAME, 0);
    }

    @Test
    public void testExecute() throws Exception {
        step.exceptionSupplier = () -> exceptionToSimulate;
        try {
            step.execute(context);
            fail();
        } catch (Exception e) {
            Mockito.verify(contextExtensionDao).addOrUpdate(PROCESS_ID, Constants.VAR_ERROR_TYPE, expectedErrorType.toString());
        }
    }

    public static class MockStep extends AbstractXS2ProcessStepWithBridge {

        private Supplier<Exception> exceptionSupplier;

        @Override
        protected ExecutionStatus pollStatusInternal(DelegateExecution context) throws Exception {
            throw exceptionSupplier.get();
        }

        @Override
        protected String getIndexVariable() {
            return INDEX_VARIABLE_NAME;
        }

    }

    @Override
    protected MockStep createStep() {
        return new MockStep();
    }

}