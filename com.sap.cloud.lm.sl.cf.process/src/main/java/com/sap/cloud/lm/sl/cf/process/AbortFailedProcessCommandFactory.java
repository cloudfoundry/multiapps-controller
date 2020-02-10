package com.sap.cloud.lm.sl.cf.process;

import java.text.MessageFormat;

import org.apache.commons.lang3.BooleanUtils;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.jobexecutor.DefaultFailedJobCommandFactory;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.HistoryUtil;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;

public class AbortFailedProcessCommandFactory extends DefaultFailedJobCommandFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbortFailedProcessCommandFactory.class);

    @Override
    public Command<Object> getCommand(String jobId, Throwable exception) {
        return new AbortFailedProcessCommand(jobId, getAbortReason(), super.getCommand(jobId, exception));
    }

    protected String getAbortReason() {
        return Operation.State.ABORTED.name();
    }

    protected static class AbortFailedProcessCommand implements Command<Object> {

        private final String jobId;
        private final String abortReason;
        private final Command<Object> delegate;

        public AbortFailedProcessCommand(String jobId, String abortReason, Command<Object> delegate) {
            this.jobId = jobId;
            this.abortReason = abortReason;
            this.delegate = delegate;
        }

        @Override
        public Object execute(CommandContext commandContext) {
            Object result = delegate.execute(commandContext);
            String processInstanceId = getProcessId();
            String correlationId = HistoryUtil.getVariableValue(commandContext, processInstanceId, Constants.VAR_CORRELATION_ID);
            if (!processInstanceId.equals(correlationId)) {
                return result;
            }

            Boolean shouldAbortOnError = HistoryUtil.getVariableValue(commandContext, processInstanceId, Constants.PARAM_ABORT_ON_ERROR);
            if (BooleanUtils.toBoolean(shouldAbortOnError)) {
                abortProcess(commandContext, processInstanceId);
            }

            return result;
        }

        private String getProcessId() {
            JobEntity job = CommandContextUtil.getJobServiceConfiguration()
                                              .getJobEntityManager()
                                              .findById(jobId);
            return job.getProcessInstanceId();
        }

        private void abortProcess(CommandContext commandContext, String processId) {
            LOGGER.info(MessageFormat.format(Messages.PROCESS_WILL_BE_AUTO_ABORTED, processId));
            RuntimeService runtimeService = getRuntimeService(commandContext);
            runtimeService.deleteProcessInstance(processId, abortReason);
        }

        private RuntimeService getRuntimeService(CommandContext commandContext) {
            return Context.getProcessEngineConfiguration(commandContext)
                          .getRuntimeService();
        }

    }

}
