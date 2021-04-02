package org.cloudfoundry.multiapps.controller.process.steps;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.client.LoggingCloudControllerClient;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.Variable;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;

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
        String spaceGuid = getVariable(Variables.SPACE_GUID);
        String correlationId = getVariable(Variables.CORRELATION_ID);
        CloudControllerClient delegate = clientProvider.getControllerClient(userName, spaceGuid, correlationId);
        return new LoggingCloudControllerClient(delegate, stepLogger);
    }

    public CloudControllerClient getControllerClient(String org, String space) {
        String userName = StepsUtil.determineCurrentUser(execution);
        String correlationId = getVariable(Variables.CORRELATION_ID);
        CloudControllerClient delegate = clientProvider.getControllerClient(userName, org, space, correlationId);
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
