package com.sap.cloud.lm.sl.cf.process.steps;

import org.flowable.engine.delegate.DelegateExecution;

public interface TaskIndexProvider {

    int getTaskIndex(DelegateExecution context);

}
