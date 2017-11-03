package com.sap.cloud.lm.sl.cf.process.steps;

import org.activiti.engine.delegate.DelegateExecution;

public interface StepIndexProvider {
    Integer getStepIndex(DelegateExecution context);
}
