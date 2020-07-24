package org.cloudfoundry.multiapps.controller.process.steps;

public interface AsyncExecution {

    AsyncExecutionState execute(ProcessContext context);

    String getPollingErrorMessage(ProcessContext context);

}
