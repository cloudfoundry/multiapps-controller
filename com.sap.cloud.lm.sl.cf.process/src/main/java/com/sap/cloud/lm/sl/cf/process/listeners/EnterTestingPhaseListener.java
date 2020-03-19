package com.sap.cloud.lm.sl.cf.process.listeners;

import javax.inject.Named;

import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.process.Messages;

@Named("enterTestingPhaseListener")
public class EnterTestingPhaseListener extends AbstractProcessExecutionListener {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(EnterTestingPhaseListener.class);

    @Override
    protected void notifyInternal(DelegateExecution execution) {
        getStepLogger().debug(Messages.ENTERING_TESTING_PHASE);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}