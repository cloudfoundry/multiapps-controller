package com.sap.cloud.lm.sl.cf.process.listeners;

import javax.inject.Named;

import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.process.Messages;

@Named("leaveTestingPhaseListener")
public class LeaveTestingPhaseListener extends AbstractProcessExecutionListener {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(LeaveTestingPhaseListener.class);

    @Override
    protected void notifyInternal(DelegateExecution context) {
        getStepLogger().debug(Messages.LEAVING_TESTING_PHASE);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
