package com.sap.cloud.lm.sl.cf.process;

import java.text.MessageFormat;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.web.api.model.State;

public class AbortFailedProcessCommandFactory extends NoJobRetryCommandFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbortFailedProcessCommandFactory.class);

    @Override
    public Command<Object> getCommand(String jobId, Throwable exception) {
        return new AbortFailedProcessCommand(jobId, getAbortReason(), super.getCommand(jobId, exception));
    }

    protected String getAbortReason() {
        return State.ABORTED.value();
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
            String processInstanceId = getProcessId(commandContext);
            HistoricVariableInstance corelationId = getHistoryService(commandContext).createHistoricVariableInstanceQuery()
                                                                                     .processInstanceId(processInstanceId)
                                                                                     .variableName(Constants.VAR_CORRELATION_ID)
                                                                                     .singleResult();
            if (!processInstanceId.equals(corelationId.getValue())) {
                return result;
            }

            HistoricVariableInstance abortOnErrorVariable = getAbortOnErrorVariable(commandContext, processInstanceId);
            if (shouldAbortProcess(abortOnErrorVariable)) {
                abortProcess(commandContext, processInstanceId);
            }

            return result;
        }

        private String getProcessId(CommandContext commandContext) {
            JobEntity job = CommandContextUtil.getJobServiceConfiguration()
                                              .getJobEntityManager()
                                              .findById(jobId);
            return job.getProcessInstanceId();
        }

        private HistoricVariableInstance getAbortOnErrorVariable(CommandContext commandContext, String processId) {
            return getHistoryService(commandContext).createHistoricVariableInstanceQuery()
                                                    .processInstanceId(processId)
                                                    .variableName(Constants.PARAM_ABORT_ON_ERROR)
                                                    .singleResult();
        }

        private boolean shouldAbortProcess(HistoricVariableInstance abortOnErrorVariable) {
            return abortOnErrorVariable != null && Boolean.TRUE.equals(abortOnErrorVariable.getValue());
        }

        private void abortProcess(CommandContext commandContext, String processId) {
            LOGGER.info(MessageFormat.format(Messages.PROCESS_WILL_BE_AUTO_ABORTED, processId));
            RuntimeService runtimeService = getRuntimeService(commandContext);
            runtimeService.deleteProcessInstance(processId, abortReason);
        }

        private HistoryService getHistoryService(CommandContext commandContext) {
            return Context.getProcessEngineConfiguration(commandContext)
                          .getHistoryService();
        }

        private RuntimeService getRuntimeService(CommandContext commandContext) {
            return Context.getProcessEngineConfiguration(commandContext)
                          .getRuntimeService();
        }

    }

}
