package org.cloudfoundry.multiapps.controller.process.flowable.commands.abort;

import org.apache.commons.lang3.BooleanUtils;
import org.cloudfoundry.multiapps.common.StepPhaseRetryException;
import org.cloudfoundry.multiapps.controller.process.flowable.commands.FlowableCommandExecutor;
import org.cloudfoundry.multiapps.controller.process.util.HistoryUtil;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbortProcessFailedJobCommand implements Command<Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbortProcessFailedJobCommand.class);

    private final AbortProcessFlowableCommandExecutorFactory abortCommandExecutorFactory;
    private final String jobId;
    private final Command<Object> delegate;

    public AbortProcessFailedJobCommand(AbortProcessFlowableCommandExecutorFactory abortCommandExecutorFactory, String jobId,
                                        Command<Object> delegate) {
        this.abortCommandExecutorFactory = abortCommandExecutorFactory;
        this.jobId = jobId;
        this.delegate = delegate;
    }

    @Override
    public Object execute(CommandContext commandContext) {
        Object result = delegate.execute(commandContext);
        try {
            abortProcessIfRequested(commandContext);
        } catch (Exception e) {
            LOGGER.warn("Auto-abort failed for job \"{}\": {}", jobId, e.getMessage(), e);
        }
        return result;
    }

    private void abortProcessIfRequested(CommandContext commandContext) {
        JobEntity job = findJob(commandContext);
        if (job == null) {
            return;
        }
        if (isRetrySignal(job)) {
            return;
        }
        abortProcessIfRequested(commandContext, job.getProcessInstanceId());
    }

    private JobEntity findJob(CommandContext commandContext) {
        return CommandContextUtil.getJobService(commandContext)
                                 .findJobById(jobId);
    }

    private boolean isRetrySignal(JobEntity job) {
        return job.getExceptionMessage() != null
            && job.getExceptionMessage().contains(StepPhaseRetryException.class.getSimpleName());
    }

    private void abortProcessIfRequested(CommandContext commandContext, String processInstanceId) {
        String correlationId = HistoryUtil.getVariableValue(commandContext, processInstanceId, Variables.CORRELATION_ID.getName());
        if (shouldAbortOnError(commandContext, correlationId)) {
            FlowableCommandExecutor abortProcessExecutor = abortCommandExecutorFactory.getExecutor(commandContext, processInstanceId);
            abortProcessExecutor.executeCommand();
        }
    }

    private boolean shouldAbortOnError(CommandContext commandContext, String processInstanceId) {
        Boolean shouldAbortOnError = HistoryUtil.getVariableValue(commandContext, processInstanceId, Variables.ABORT_ON_ERROR.getName());
        return BooleanUtils.toBoolean(shouldAbortOnError);
    }
}
