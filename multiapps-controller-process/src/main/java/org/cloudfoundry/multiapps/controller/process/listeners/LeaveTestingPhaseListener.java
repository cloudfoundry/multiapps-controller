package org.cloudfoundry.multiapps.controller.process.listeners;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.process.Messages;
import org.flowable.engine.delegate.DelegateExecution;

@Named("leaveTestingPhaseListener")
public class LeaveTestingPhaseListener extends AbstractProcessExecutionListener {

    private static final long serialVersionUID = 1L;

    @Override
    protected void notifyInternal(DelegateExecution execution) {
        getStepLogger().debug(Messages.LEAVING_TESTING_PHASE);
    }

}
