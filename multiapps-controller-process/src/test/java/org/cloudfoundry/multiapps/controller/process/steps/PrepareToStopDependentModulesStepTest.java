package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.Collections;
import java.util.List;

import org.cloudfoundry.multiapps.controller.core.model.SubprocessPhase;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PrepareToStopDependentModulesStepTest extends SyncFlowableStepTest<PrepareToStopDependentModulesStep> {

    @Test
    void testPrepareToStopDependentModulesStepEmpty() {
        context.setVariable(Variables.DEPENDENT_MODULES_TO_STOP, Collections.emptyList());
        step.execute(execution);
        assertStepFinishedSuccessfully();
        Assertions.assertEquals(0, context.getVariable(Variables.APPS_TO_STOP_INDEX));
        Assertions.assertEquals(SubprocessPhase.BEFORE_APPLICATION_STOP, context.getVariable(Variables.SUBPROCESS_PHASE));
        Assertions.assertEquals(0, context.getVariable(Variables.APPS_TO_STOP_COUNT));
        Assertions.assertEquals(Variables.APPS_TO_STOP_INDEX.getName(), context.getVariable(Variables.INDEX_VARIABLE_NAME));
    }

    @Test
    void testPrepareToStopDependentModulesStepTest() {
        context.setVariable(Variables.DEPENDENT_MODULES_TO_STOP, List.of(Module.createV3()
                                                                               .setName("module")));
        step.execute(execution);
        assertStepFinishedSuccessfully();
        Assertions.assertEquals(0, context.getVariable(Variables.APPS_TO_STOP_INDEX));
        Assertions.assertEquals(SubprocessPhase.BEFORE_APPLICATION_STOP, context.getVariable(Variables.SUBPROCESS_PHASE));
        Assertions.assertEquals(1, context.getVariable(Variables.APPS_TO_STOP_COUNT));
        Assertions.assertEquals(Variables.APPS_TO_STOP_INDEX.getName(), context.getVariable(Variables.INDEX_VARIABLE_NAME));
    }

    @Override
    protected PrepareToStopDependentModulesStep createStep() {
        return new PrepareToStopDependentModulesStep();
    }
}