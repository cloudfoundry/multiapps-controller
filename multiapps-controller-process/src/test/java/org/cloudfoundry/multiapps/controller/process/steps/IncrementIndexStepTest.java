package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class IncrementIndexStepTest extends SyncFlowableStepTest<IncrementIndexStep> {

    private final int index;

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] { { 1 }, { 2 }, { 3 }, { 4 }, { 5 } });
    }

    public IncrementIndexStepTest(int index) {
        this.index = index;
    }

    @Before
    public void setUp() {
        prepareContext();
    }

    @Test
    public void testExecute() {
        step.execute(execution);

        assertStepFinishedSuccessfully();

        String indexVariableName = context.getVariable(Variables.INDEX_VARIABLE_NAME);
        assertEquals(index + 1, execution.getVariable(indexVariableName));
    }

    private DelegateExecution prepareContext() {
        context.setVariable(Variables.INDEX_VARIABLE_NAME, Variables.MODULES_INDEX.getName());
        context.setVariable(Variables.MODULES_INDEX, index);
        return execution;
    }

    @Override
    protected IncrementIndexStep createStep() {
        return new IncrementIndexStep();
    }

}
