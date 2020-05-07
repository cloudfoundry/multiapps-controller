package com.sap.cloud.lm.sl.cf.process.listeners;

import javax.inject.Named;

import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.process.Messages;

@Named("leaveTestingPhaseListener")
public class LeaveTestingPhaseListener extends AbstractProcessExecutionListener {

    private static final long serialVersionUID = 1L;

    @Override
    protected void notifyInternal(DelegateExecution execution) {
        getStepLogger().debug(Messages.LEAVING_TESTING_PHASE);
    }

}
