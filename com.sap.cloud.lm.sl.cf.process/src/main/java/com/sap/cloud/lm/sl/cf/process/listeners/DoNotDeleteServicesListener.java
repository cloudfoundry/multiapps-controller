package com.sap.cloud.lm.sl.cf.process.listeners;

import javax.inject.Named;

import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Named("doNotDeleteServicesListener")
public class DoNotDeleteServicesListener extends AbstractProcessExecutionListener {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(DoNotDeleteServicesListener.class);

    @Override
    protected void notifyInternal(DelegateExecution context) {
        getStepLogger().warn(Messages.SKIP_SERVICES_DELETION);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
