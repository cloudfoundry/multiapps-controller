package com.sap.cloud.lm.sl.cf.process.steps;

import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.client.LoggingCloudControllerClient;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;
import com.sap.cloud.lm.sl.cf.process.variables.Variable;
import com.sap.cloud.lm.sl.cf.process.variables.VariableHandling;
import com.sap.cloud.lm.sl.common.SLException;

public class ProcessContext {

    private final DelegateExecution execution;
    private final StepLogger stepLogger;
    private final CloudControllerClientProvider clientProvider;

    public ProcessContext(DelegateExecution execution, StepLogger stepLogger, CloudControllerClientProvider clientProvider) {
        this.execution = execution;
        this.stepLogger = stepLogger;
        this.clientProvider = clientProvider;
    }

    public DelegateExecution getExecution() {
        return execution;
    }

    public StepLogger getStepLogger() {
        return stepLogger;
    }

    public CloudControllerClient getControllerClient() {
        String userName = StepsUtil.determineCurrentUser(execution);
        String spaceId = getVariable(Variables.SPACE_ID);
        CloudControllerClient delegate = clientProvider.getControllerClient(userName, spaceId);
        return new LoggingCloudControllerClient(delegate, stepLogger);
    }

    public CloudControllerClient getControllerClient(String org, String space) {
        String userName = StepsUtil.determineCurrentUser(execution);
        CloudControllerClient delegate = clientProvider.getControllerClient(userName, org, space, execution.getProcessInstanceId());
        return new LoggingCloudControllerClient(delegate, stepLogger);
    }

    public <T> T getRequiredVariable(Variable<T> variable) {
        T value = getVariable(variable);
        if (value == null) {
            throw new SLException(Messages.REQUIRED_PROCESS_VARIABLE_IS_MISSING, variable.getName());
        }
        return value;
    }

    public <T> T getVariable(Variable<T> variable) {
        return VariableHandling.get(execution, variable);
    }

    public <T> void setVariable(Variable<T> variable, T value) {
        VariableHandling.set(execution, variable, value);
    }

    public void removeVariable(Variable<?> variable) {
        VariableHandling.remove(execution, variable);
    }

}
