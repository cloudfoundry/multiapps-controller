package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.model.ErrorType;
import org.cloudfoundry.multiapps.controller.persistence.model.HistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableProgressMessage;
import org.cloudfoundry.multiapps.controller.persistence.model.ProgressMessage.ProgressMessageType;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLogger;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLogsPersister;
import org.cloudfoundry.multiapps.controller.persistence.services.ProgressMessageService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ProcessHelper;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.runtime.Execution;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Value.Immutable
public abstract class ProcessStepHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessStepHelper.class);

    protected void postExecuteStep(ProcessContext context, StepPhase state) {
        logDebug(MessageFormat.format(Messages.STEP_FINISHED, context.getExecution()
                                                                     .getCurrentFlowElement()
                                                                     .getName()));

        getProcessLogsPersister().persistLogs(context.getVariable(Variables.CORRELATION_ID), context.getVariable(Variables.TASK_ID));
        context.setVariable(Variables.STEP_EXECUTION, state.toString());
    }

    void preExecuteStep(ProcessContext context, StepPhase initialPhase) {
        String taskId = context.getExecution()
                               .getCurrentActivityId();
        context.setVariable(Variables.TASK_ID, taskId);

        deletePreviousErrorType(context);
        getStepLogger().logFlowableTask();
        context.setVariable(Variables.STEP_PHASE, initialPhase);
    }

    protected void deletePreviousErrorType(ProcessContext context) {
        String processId = context.getExecution()
                                  .getProcessInstanceId();
        ErrorType errorType = context.getVariable(Variables.ERROR_TYPE);
        if (errorType == null) {
            return;
        }
        LOGGER.debug(MessageFormat.format(Messages.DELETING_ERROR_TYPE_O_FOR_PROCESS_1, errorType, processId));
        context.removeVariable(Variables.ERROR_TYPE);
    }

    protected void logExceptionAndStoreProgressMessage(ProcessContext context, Throwable t) {
        logException(context, t);
        storeExceptionInProgressMessageService(context, t);
    }

    private void logException(ProcessContext context, Throwable t) {
        LOGGER.error(Messages.EXCEPTION_CAUGHT, t);
        getProcessLogger().error(Messages.EXCEPTION_CAUGHT, t);

        if (t instanceof ContentException) {
            context.setVariable(Variables.ERROR_TYPE, ErrorType.CONTENT_ERROR);
        } else {
            context.setVariable(Variables.ERROR_TYPE, ErrorType.UNKNOWN_ERROR);
        }
    }

    private void storeExceptionInProgressMessageService(ProcessContext context, Throwable throwable) {
        try {
            getProgressMessageService().add(ImmutableProgressMessage.builder()
                                                                    .processId(context.getVariable(Variables.CORRELATION_ID))
                                                                    .taskId(getCurrentActivityId(context.getExecution()))
                                                                    .type(ProgressMessageType.ERROR)
                                                                    .text(throwable.getMessage())
                                                                    .build());
        } catch (SLException e) {
            getProcessLogger().error(Messages.SAVING_ERROR_MESSAGE_FAILED, e);
        }
    }

    // This method is needed because sometimes the DelegateExecution::getCurrentActivityId returns null
    // Check the issue: https://github.com/flowable/flowable-engine/issues/1280
    private String getCurrentActivityId(DelegateExecution execution) {
        List<Execution> processExecutions = getProcessEngineConfiguration().getRuntimeService()
                                                                           .createExecutionQuery()
                                                                           .processInstanceId(execution.getProcessInstanceId())
                                                                           .list();
        List<Execution> processExecutionsWithActivityIds = processExecutions.stream()
                                                                            .filter(e -> e.getActivityId() != null)
                                                                            .collect(Collectors.toList());
        if (processExecutionsWithActivityIds.isEmpty()) {
            // if this happen then there is a really big problem with Flowable :)
            throw new SLException("There are no executions for process with id: " + execution.getProcessInstanceId());
        }
        return processExecutionsWithActivityIds.get(0)
                                               .getActivityId();
    }

    private void logDebug(String message) {
        getProcessLogger().debug(message);
    }

    private ProcessLogger getProcessLogger() {
        return getStepLogger().getProcessLogger();
    }

    public void failStepIfProcessIsAborted(ProcessContext context) {
        String correlationId = context.getVariable(Variables.CORRELATION_ID);
        List<HistoricOperationEvent> historicOperationEvents = getProcessHelper().getHistoricOperationEventByProcessId(correlationId);
        if (isProcessAborted(historicOperationEvents)) {
            throw new SLException(Messages.PROCESS_WAS_ABORTED);
        }
    }

    public static boolean isProcessAborted(List<HistoricOperationEvent> historicOperationEvents) {
        return historicOperationEvents.stream()
                                      .map(HistoricOperationEvent::getType)
                                      .anyMatch(HistoricOperationEvent.EventType.ABORT_EXECUTED::equals);
    }

    public abstract ProgressMessageService getProgressMessageService();

    public abstract ProcessLogsPersister getProcessLogsPersister();

    public abstract StepLogger getStepLogger();

    public abstract ProcessEngineConfiguration getProcessEngineConfiguration();

    public abstract ProcessHelper getProcessHelper();

}
