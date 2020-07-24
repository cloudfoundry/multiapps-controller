package com.sap.cloud.lm.sl.cf.process.listeners;

import javax.inject.Named;

import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.process.Messages;

@Named("doNotDeleteServicesListener")
public class DoNotDeleteServicesListener extends AbstractProcessExecutionListener {

    private static final long serialVersionUID = 1L;

    @Override
    protected void notifyInternal(DelegateExecution execution) {
        if (!isRootProcess(execution)) {
            return;
        }
        getStepLogger().warn(Messages.SKIP_SERVICES_DELETION);
    }

}
