package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class IncrementIndexStepTest extends SyncFlowableStepTest<IncrementIndexStep> {

    public static Stream<Arguments> testExecute() {
        return Stream.of(
        // @formatter:off
                Arguments.of(1),
                Arguments.of(2),
                Arguments.of(3),
                Arguments.of(4),
                Arguments.of(5)
        // @formatter:off
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExecute(int index) {
        prepareContext(index);
        step.execute(execution);

        assertStepFinishedSuccessfully();

        String indexVariableName = context.getVariable(Variables.INDEX_VARIABLE_NAME);
        assertEquals(index + 1, execution.getVariable(indexVariableName));
    }

    private void prepareContext(int index) {
        context.setVariable(Variables.INDEX_VARIABLE_NAME, Variables.MODULES_INDEX.getName());
        context.setVariable(Variables.MODULES_INDEX, index);
    }

    @Override
    protected IncrementIndexStep createStep() {
        return new IncrementIndexStep();
    }

}
