package com.sap.cloud.lm.sl.cf.process.steps;

import com.sap.cloud.lm.sl.cf.core.activiti.ActivitiFacade;
import com.sap.cloud.lm.sl.common.SLException;

public abstract class TimeoutSubProcessMonitorExecution extends AbstractSubProcessMonitorExecution {

    public TimeoutSubProcessMonitorExecution(ActivitiFacade activitiFacade) {
        super(activitiFacade);
    }

    @Override
    public AsyncExecutionState execute(ExecutionWrapper execution) {
        try {
            Thread.sleep(10000);
            return super.execute(execution);
        } catch (InterruptedException e) {
            throw new SLException(e);
        }
    }

}
