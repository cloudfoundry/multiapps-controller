package com.sap.cloud.lm.sl.cf.process.steps;

public interface AsyncExecution {

    AsyncExecutionState execute(ExecutionWrapper execution);

}
