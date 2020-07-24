package com.sap.cloud.lm.sl.cf.process.listeners;

import javax.inject.Named;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;

import com.sap.cloud.lm.sl.cf.core.model.Phase;
import com.sap.cloud.lm.sl.cf.process.variables.VariableHandling;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

@Named("setResumePhase")
public class SetAfterResumePhaseListener implements ExecutionListener {

    private static final long serialVersionUID = 1L;

    @Override
    public void notify(DelegateExecution execution) {
        VariableHandling.set(execution, Variables.PHASE, Phase.AFTER_RESUME);
    }
}
