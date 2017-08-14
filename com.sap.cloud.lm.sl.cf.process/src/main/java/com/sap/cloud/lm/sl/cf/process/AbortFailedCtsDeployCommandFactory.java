package com.sap.cloud.lm.sl.cf.process;

import java.text.MessageFormat;

import org.activiti.engine.HistoryService;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.history.HistoricVariableInstanceQuery;
import org.activiti.engine.impl.cfg.TransactionContext;
import org.activiti.engine.impl.cfg.TransactionState;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.activiti.common.NoJobRetryCommandFactory;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.lmsl.slp.SlpTaskState;

public class AbortFailedCtsDeployCommandFactory extends NoJobRetryCommandFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbortFailedCtsDeployCommandFactory.class);

    @Override
    public Command<Object> getCommand(String jobId, Throwable exception) {
        return new CommandDelegator(jobId, super.getCommand(jobId, exception));
    }

    private class CommandDelegator implements Command<Object> {

        private final String jobId;
        private final Command<Object> command;

        public CommandDelegator(String jobId, Command<Object> command) {
            this.jobId = jobId;
            this.command = command;
        }

        @Override
        public Object execute(CommandContext commandContext) {
            Object result = (command.execute(commandContext));
            JobEntity job = (commandContext.getJobEntityManager().findJobById(jobId));
            ProcessEngineConfiguration processEngineConfiguration = commandContext.getProcessEngineConfiguration();
            RuntimeService runtimeService = processEngineConfiguration.getRuntimeService();
            HistoryService historyService = processEngineConfiguration.getHistoryService();
            String processInstanceId = job.getProcessInstanceId();

            HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();
            query.processInstanceId(processInstanceId);
            query.variableName(com.sap.cloud.lm.sl.slp.Constants.VARIABLE_NAME_SERVICE_ID);
            HistoricVariableInstance serviceIdVariable = query.singleResult();
            if (isCtsDeploy(serviceIdVariable)) {
                LOGGER.info(MessageFormat.format(Messages.PROCESS_WILL_BE_ABORTED, processInstanceId));
                TransactionContext transactionContext = commandContext.getTransactionContext();
                transactionContext.addTransactionListener(TransactionState.COMMITTED, (context) -> {
                    runtimeService.deleteProcessInstance(processInstanceId, SlpTaskState.SLP_TASK_STATE_ABORTED.value());
                });
            }
            return result;
        }

        private boolean isCtsDeploy(HistoricVariableInstance serviceIdVariable) {
            return serviceIdVariable != null && Constants.CTS_DEPLOY_SERVICE_ID.equals(serviceIdVariable.getValue());
        }

    }

}
