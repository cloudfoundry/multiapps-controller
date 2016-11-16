package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.activiti.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.process.Constants;

@RunWith(Parameterized.class)
public class IncrementAppIndexStepTest extends AbstractStepTest<IncrementAppIndexStep> {

    private final int index;

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] { { 1 }, { 2 }, { 3 }, { 4 }, { 5 } });
    }

    public IncrementAppIndexStepTest(int index) {
        this.index = index;
    }

    @Before
    public void setUp() throws Exception {
        prepareContext();
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertEquals(ExecutionStatus.SUCCESS.toString(),
            context.getVariable(com.sap.activiti.common.Constants.STEP_NAME_PREFIX + step.getLogicalStepName()));

        assertEquals(index + 1, context.getVariable(Constants.VAR_APPS_INDEX));
    }

    private DelegateExecution prepareContext() {
        context.setVariable(com.sap.cloud.lm.sl.cf.process.Constants.VAR_APPS_INDEX, index);
        return context;
    }

    @Override
    protected IncrementAppIndexStep createStep() {
        return new IncrementAppIndexStep();
    }

}
