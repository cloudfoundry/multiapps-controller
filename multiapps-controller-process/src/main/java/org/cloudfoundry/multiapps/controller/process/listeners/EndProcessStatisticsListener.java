package org.cloudfoundry.multiapps.controller.process.listeners;

import javax.inject.Named;

import org.flowable.engine.delegate.DelegateExecution;

@Named("endProcessStatisticsListener")
public class EndProcessStatisticsListener extends AbstractProcessExecutionListener {

    private static final long serialVersionUID = 1L;

    @Override
    protected void notifyInternal(DelegateExecution execution) throws Exception {
    }

}
