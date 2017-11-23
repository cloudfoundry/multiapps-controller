package com.sap.cloud.lm.sl.cf.process.steps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AsyncExecution {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    public abstract AsyncExecutionState execute(ExecutionWrapper execution) throws Exception;

}
