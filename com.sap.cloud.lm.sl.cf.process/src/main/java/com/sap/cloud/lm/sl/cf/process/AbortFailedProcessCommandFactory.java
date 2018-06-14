package com.sap.cloud.lm.sl.cf.process;

import java.text.MessageFormat;

import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.impl.cfg.TransactionContext;
import org.activiti.engine.impl.cfg.TransactionState;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.JobEntity;
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
            HistoricVariableInstance abortOnErrorVariable = getAbortOnErrorVariable(commandContext, processInstanceId);
            if (shouldAbortProcess(abortOnErrorVariable)) {
                abortProcess(commandContext, processInstanceId);
            }

            return result;
        }

        private String getProcessId(CommandContext commandContext) {
            JobEntity job = commandContext.getJobEntityManager()
                .findJobById(jobId);
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
            TransactionContext transactionContext = commandContext.getTransactionContext();
            transactionContext.addTransactionListener(TransactionState.COMMITTED,
                context -> runtimeService.deleteProcessInstance(processId, abortReason));
        }

        private HistoryService getHistoryService(CommandContext commandContext) {
            return commandContext.getProcessEngineConfiguration()
                .getHistoryService();
        }

        private RuntimeService getRuntimeService(CommandContext commandContext) {
            return commandContext.getProcessEngineConfiguration()
                .getRuntimeService();
        }

    }

}
