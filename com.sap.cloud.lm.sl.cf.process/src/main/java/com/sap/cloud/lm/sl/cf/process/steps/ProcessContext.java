package com.sap.cloud.lm.sl.cf.process.steps;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.process.client.LoggingCloudControllerClient;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;
import com.sap.cloud.lm.sl.cf.process.variables.Variable;
import com.sap.cloud.lm.sl.cf.process.variables.VariablesHandler;

public class ProcessContext {

    private final DelegateExecution execution;
    private final StepLogger stepLogger;
    private final CloudControllerClientProvider clientProvider;
    private final VariablesHandler variablesHandler;

    public ProcessContext(DelegateExecution execution, StepLogger stepLogger, CloudControllerClientProvider clientProvider) {
        this.execution = execution;
        this.stepLogger = stepLogger;
        this.clientProvider = clientProvider;
        this.variablesHandler = new VariablesHandler(execution);
    }

    public DelegateExecution getExecution() {
        return execution;
    }

    public StepLogger getStepLogger() {
        return stepLogger;
    }

    public CloudControllerClient getControllerClient() {
        CloudControllerClient delegate = StepsUtil.getControllerClient(execution, clientProvider);
        return new LoggingCloudControllerClient(delegate, stepLogger);
    }

    public CloudControllerClient getControllerClient(String org, String space) {
        CloudControllerClient delegate = StepsUtil.getControllerClient(execution, clientProvider, org, space);
        return new LoggingCloudControllerClient(delegate, stepLogger);
    }

    public <T> T getVariable(Variable<T> variable) {
        return variablesHandler.get(variable);
    }

    public <T> void setVariable(Variable<T> variable, T value) {
        variablesHandler.set(variable, value);
    }

}
