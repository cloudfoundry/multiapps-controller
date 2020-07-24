package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.common.SLException;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.runtime.Execution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.model.ErrorType;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ProgressMessageService;
import com.sap.cloud.lm.sl.cf.persistence.model.ImmutableProgressMessage;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage.ProgressMessageType;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLogger;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLogsPersister;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

public class ProcessStepHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessStepHelper.class);

    private final ProgressMessageService progressMessageService;
    private final ProcessLogsPersister processLogsPersister;
    private final StepLogger stepLogger;

    private final ProcessEngineConfiguration processEngineConfiguration;

    public ProcessStepHelper(ProgressMessageService progressMessageService, StepLogger stepLogger,
                             ProcessLogsPersister processLogsPersister, ProcessEngineConfiguration processEngineConfigurationSupplier) {
        this.progressMessageService = progressMessageService;
        this.stepLogger = stepLogger;
        this.processLogsPersister = processLogsPersister;
        this.processEngineConfiguration = processEngineConfigurationSupplier;
    }

    protected void postExecuteStep(ProcessContext context, StepPhase state) {
        logDebug(MessageFormat.format(Messages.STEP_FINISHED, context.getExecution()
                                                                     .getCurrentFlowElement()
                                                                     .getName()));

        processLogsPersister.persistLogs(context.getVariable(Variables.CORRELATION_ID), context.getVariable(Variables.TASK_ID));
        context.setVariable(Variables.STEP_EXECUTION, state.toString());
    }

    void preExecuteStep(ProcessContext context, StepPhase initialPhase) {
        String taskId = context.getExecution()
                               .getCurrentActivityId();
        context.setVariable(Variables.TASK_ID, taskId);

        deletePreviousErrorType(context);
        stepLogger.logFlowableTask();
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
            progressMessageService.add(ImmutableProgressMessage.builder()
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
        List<Execution> processExecutions = processEngineConfiguration.getRuntimeService()
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
        return stepLogger.getProcessLogger();
    }

    public void failStepIfProcessIsAborted(ProcessContext context) {
        Boolean processAborted = (Boolean) processEngineConfiguration.getRuntimeService()
                                                                     .getVariable(context.getVariable(Variables.CORRELATION_ID),
                                                                                  Constants.PROCESS_ABORTED);
        if (processAborted != null && processAborted) {
            throw new SLException(Messages.PROCESS_WAS_ABORTED);
        }
    }

}
