package com.sap.cloud.lm.sl.cf.process.steps;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;

public class ExecutionWrapper {
    private final DelegateExecution context;
    private final StepLogger stepLogger;
    private final CloudControllerClientProvider clientProvider;

    public ExecutionWrapper(DelegateExecution context, StepLogger stepLogger, CloudControllerClientProvider clientProvider) {
        this.context = context;
        this.stepLogger = stepLogger;
        this.clientProvider = clientProvider;
    }

    public DelegateExecution getContext() {
        return context;
    }

    public StepLogger getStepLogger() {
        return stepLogger;
    }

    public CloudControllerClient getControllerClient() {
        return StepsUtil.getControllerClient(context, clientProvider, stepLogger);
    }

    public CloudControllerClient getControllerClient(String org, String space) {
        return StepsUtil.getControllerClient(context, clientProvider, stepLogger, org, space);
    }

}
