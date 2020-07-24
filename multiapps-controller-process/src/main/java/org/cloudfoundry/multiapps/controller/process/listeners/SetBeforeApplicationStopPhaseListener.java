package org.cloudfoundry.multiapps.controller.process.listeners;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.model.SubprocessPhase;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;

@Named("setBeforeApplicationStopPhase")
public class SetBeforeApplicationStopPhaseListener implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) {
        VariableHandling.set(execution, Variables.SUBPROCESS_PHASE, SubprocessPhase.BEFORE_APPLICATION_STOP);
    }

}
