package com.sap.cloud.lm.sl.cf.process.steps;

public interface AsyncExecution {

    AsyncExecutionState execute(ExecutionWrapper execution);
    
    void onPollingError(ExecutionWrapper execution, Exception e) throws Exception;

}
