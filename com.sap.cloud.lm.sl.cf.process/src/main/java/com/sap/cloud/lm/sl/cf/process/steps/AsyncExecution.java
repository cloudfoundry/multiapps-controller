package com.sap.cloud.lm.sl.cf.process.steps;

public interface AsyncExecution {

    AsyncExecutionState execute(ProcessContext context);

    String getPollingErrorMessage(ProcessContext context);

}
