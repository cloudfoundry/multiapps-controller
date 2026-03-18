package org.cloudfoundry.multiapps.controller.process.listeners;

import jakarta.inject.Inject;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.model.ExternalLoggingServiceConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableLoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.services.HistoricOperationEventService;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationLogsExporter;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLogger;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLoggerPersister;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLoggerProvider;
import org.cloudfoundry.multiapps.controller.persistence.services.ProgressMessageService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.cloudfoundry.multiapps.controller.process.services.CloudLoggingServiceLogsProvider;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractProcessExecutionListener implements ExecutionListener {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractProcessExecutionListener.class);

    private final ProgressMessageService progressMessageService;
    private final StepLogger.Factory stepLoggerFactory;
    private final ProcessLoggerProvider processLoggerProvider;
    private final HistoricOperationEventService historicOperationEventService;
    private final FlowableFacade flowableFacade;
    protected final ApplicationConfiguration configuration;
    private final ProcessLoggerPersister processLoggerPersister;

    private StepLogger stepLogger;
    private final OperationLogsExporter operationLogsExporter;
    private final CloudLoggingServiceLogsProvider cloudLoggingServiceLogsProvider;

    @Inject
    protected AbstractProcessExecutionListener(ProgressMessageService progressMessageService, StepLogger.Factory stepLoggerFactory,
                                               ProcessLoggerProvider processLoggerProvider, ProcessLoggerPersister processLoggerPersister,
                                               HistoricOperationEventService historicOperationEventService, FlowableFacade flowableFacade,
                                               ApplicationConfiguration configuration, OperationLogsExporter operationLogsExporter,
                                               CloudLoggingServiceLogsProvider cloudLoggingServiceLogsProvider) {
        this.progressMessageService = progressMessageService;
        this.stepLoggerFactory = stepLoggerFactory;
        this.processLoggerProvider = processLoggerProvider;
        this.processLoggerPersister = processLoggerPersister;
        this.historicOperationEventService = historicOperationEventService;
        this.flowableFacade = flowableFacade;
        this.configuration = configuration;
        this.operationLogsExporter = operationLogsExporter;
        this.cloudLoggingServiceLogsProvider = cloudLoggingServiceLogsProvider;
    }

    @Override
    public void notify(DelegateExecution execution) {
        initializeMustHaveVariables(execution);
        try {
            this.stepLogger = createStepLogger(execution);
            notifyInternal(execution);
        } catch (Exception e) {
            logException(e, Messages.EXECUTION_OF_PROCESS_LISTENER_HAS_FAILED);
            throw new SLException(e, Messages.EXECUTION_OF_PROCESS_LISTENER_HAS_FAILED);
        } finally {
            finalizeLogs(execution);
        }
    }

    protected void finalizeLogs(DelegateExecution execution) {
        String correlationId = VariableHandling.get(execution, Variables.CORRELATION_ID);
        String taskId = VariableHandling.get(execution, Variables.TASK_ID);
        DeploymentDescriptor deploymentDescriptor = VariableHandling.get(execution, Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR);

        if (VariableHandling.get(execution,
                                 Variables.EXTERNAL_LOGGING_SERVICE_WEB_CLIENT) != null) {
            ExternalLoggingServiceConfiguration externalLoggingServiceConfiguration = VariableHandling.get(execution,
                                                                                                           Variables.EXTERNAL_LOGGING_SERVICE_WEB_CLIENT);

            LoggingConfiguration loggingConfiguration = ImmutableLoggingConfiguration.builder()
                                                                                     .endpointUrl(
                                                                                         externalLoggingServiceConfiguration.getEndpointUrl())
                                                                                     .targetSpace(
                                                                                         externalLoggingServiceConfiguration.getTargetSpace())
                                                                                     .clientCert(
                                                                                         externalLoggingServiceConfiguration.getClientCert())
                                                                                     .clientKey(
                                                                                         externalLoggingServiceConfiguration.getClientKey())
                                                                                     .targetOrg(
                                                                                         externalLoggingServiceConfiguration.getTargetOrg())
                                                                                     .serverCa(
                                                                                         externalLoggingServiceConfiguration.getServerCa())
                                                                                     .operationId(correlationId)
                                                                                     .build();

            processLoggerPersister.persistLogs(loggingConfiguration, correlationId, taskId);
        } else {
            processLoggerPersister.persistLogs(null, correlationId, taskId);
        }
    }

    private void initializeMustHaveVariables(DelegateExecution execution) {
        try {
            initializeCorrelationId(execution);
            initializeTaskId(execution);
        } catch (Exception e) {
            LOGGER.error(Messages.EXECUTION_OF_PROCESS_LISTENER_HAS_FAILED, e);
            throw new SLException(e, Messages.EXECUTION_OF_PROCESS_LISTENER_HAS_FAILED);
        }
    }

    private void initializeCorrelationId(DelegateExecution execution) {
        String correlationId = VariableHandling.get(execution, Variables.CORRELATION_ID);
        if (correlationId == null) {
            VariableHandling.set(execution, Variables.CORRELATION_ID, execution.getProcessInstanceId());
        }
    }

    private void initializeTaskId(DelegateExecution execution) {
        String taskId = VariableHandling.get(execution, Variables.TASK_ID);
        if (taskId == null) {
            VariableHandling.set(execution, Variables.TASK_ID, execution.getCurrentActivityId());
        }
    }

    protected void logException(Exception e, String message) {
        LOGGER.error(message, e);
        getProcessLogger().error(message, e);
    }

    protected ProcessLogger getProcessLogger() {
        return getStepLogger().getProcessLogger();
    }

    protected StepLogger getStepLogger() {
        if (stepLogger == null) {
            throw new IllegalStateException(Messages.STEP_LOGGER_NOT_INITIALIZED);
        }
        return stepLogger;
    }

    protected HistoricOperationEventService getHistoricOperationEventService() {
        return historicOperationEventService;
    }

    private StepLogger createStepLogger(DelegateExecution execution) {
        return stepLoggerFactory.create(execution, progressMessageService, processLoggerProvider, getLogger(),
                                        cloudLoggingServiceLogsProvider);
    }

    protected boolean isRootProcess(DelegateExecution execution) {
        String correlationId = VariableHandling.get(execution, Variables.CORRELATION_ID);
        String processInstanceId = execution.getProcessInstanceId();
        return processInstanceId.equals(correlationId);
    }

    protected void setVariableInParentProcess(DelegateExecution execution, String variableName, Object value) {
        flowableFacade.setVariableInParentProcess(execution, variableName, value);
    }

    protected abstract void notifyInternal(DelegateExecution execution) throws Exception;

    protected Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }

}
