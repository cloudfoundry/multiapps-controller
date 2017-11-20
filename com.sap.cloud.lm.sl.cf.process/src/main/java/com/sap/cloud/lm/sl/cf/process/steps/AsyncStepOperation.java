package com.sap.cloud.lm.sl.cf.process.steps;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.activiti.common.Logger;

public abstract class AsyncStepOperation {

    protected final Logger LOGGER = Logger.getInstance(getClass());

    public abstract ExecutionStatus executeOperation(ExecutionWrapper execution) throws Exception;

}
