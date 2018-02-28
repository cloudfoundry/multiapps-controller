package com.sap.cloud.lm.sl.cf.process.steps;

import org.activiti.engine.delegate.DelegateExecution;

public interface TaskIndexProvider {

    int getTaskIndex(DelegateExecution context);

}
