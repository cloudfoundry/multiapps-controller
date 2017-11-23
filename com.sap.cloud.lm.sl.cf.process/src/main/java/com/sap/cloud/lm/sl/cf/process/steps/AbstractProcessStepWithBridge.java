// package com.sap.cloud.lm.sl.cf.process.steps;
//
// import org.activiti.engine.delegate.DelegateExecution;
//
// import com.sap.activiti.common.ExecutionStatus;
//
// public abstract class AbstractProcessStepWithBridge extends AbstractProcessStep {
//
// @Override
// protected ExecutionStatus getExecutionStatus(DelegateExecution context) {
// String statusVariable = (String) context.getVariable(getStatusVariable());
// return statusVariable != null ? ExecutionStatus.valueOf(statusVariable) : ExecutionStatus.NEW;
// }
// }