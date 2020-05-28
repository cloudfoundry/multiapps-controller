package com.sap.cloud.lm.sl.cf.process.listeners;

import javax.inject.Named;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;

import com.sap.cloud.lm.sl.cf.core.model.SubprocessPhase;
import com.sap.cloud.lm.sl.cf.process.variables.VariableHandling;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

@Named("setBeforeApplicationStopPhase")
public class SetBeforeApplicationStopPhaseListener implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) {
        VariableHandling.set(execution, Variables.SUBPROCESS_PHASE, SubprocessPhase.BEFORE_APPLICATION_STOP);
    }

}
