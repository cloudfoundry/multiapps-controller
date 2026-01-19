package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.Collections;
import java.util.List;

import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.api.Test;

class PrepareToStopDependentModulesStepTest extends SyncFlowableStepTest<PrepareToStopDependentModulesStep> {

    @Test
    void testPrepareToStopDependentModulesStepEmpty() {
        context.setVariable(Variables.DEPENDENT_MODULES_TO_STOP, Collections.emptyList());
        step.execute(execution);
        context.setVariable(Variables.APPS_TO_STOP_COUNT, 0);
        assertStepFinishedSuccessfully();
    }

    @Test
    void testPrepareToStopDependentModulesStepTest() {
        context.setVariable(Variables.DEPENDENT_MODULES_TO_STOP, List.of(Module.createV3()
                                                                               .setName("module")));
        step.execute(execution);
        context.setVariable(Variables.APPS_TO_STOP_COUNT, 1);
        assertStepFinishedSuccessfully();
    }

    @Override
    protected PrepareToStopDependentModulesStep createStep() {
        return new PrepareToStopDependentModulesStep();
    }
}