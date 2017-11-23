package com.sap.cloud.lm.sl.cf.process;

import org.activiti.engine.impl.cfg.TransactionContext;
import org.activiti.engine.impl.cfg.TransactionListener;
import org.activiti.engine.impl.cfg.TransactionState;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.jobexecutor.FailedJobCommandFactory;
import org.activiti.engine.impl.jobexecutor.JobExecutor;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Sets number of retries on failed Activiti job. By default Activiti will retry failed jobs three times. Responsible also for set of
 * exception and stack trace if availalble.
 */
public class NoJobRetryCommandFactory implements FailedJobCommandFactory {

    @Override
    public Command<Object> getCommand(String jobId, Throwable exception) {
        return new NoJobRetryCommand(jobId, exception);
    }

    private static class NoJobRetryCommand implements Command<Object> {

        private static final int NO_RETRIES = 0;
        private String jobId;
        private Throwable exception;

        public NoJobRetryCommand(String jobId, Throwable exception) {
            this.jobId = jobId;
            this.exception = exception;
        }

        @Override
        public Object execute(CommandContext commandContext) {
            JobEntity job = Context.getCommandContext().getJobEntityManager().findJobById(jobId);
            job.setRetries(NO_RETRIES);
            job.setLockOwner(null);
            job.setLockExpirationTime(null);

            if (exception != null) {
                job.setExceptionMessage(exception.getMessage());
                job.setExceptionStacktrace(getExceptionStacktrace());
            }

            addTransactionListener(commandContext);
            return null;
        }

        private String getExceptionStacktrace() {
            return ExceptionUtils.getStackTrace(exception);
        }

        /*
         * In version 5.16.0 of activiti-engine, the class MessageAddedNotification is renamed to JobAddedNotification. This leads to class
         * not def found exceptions when adopting version > 5.16.0 of the activiti engine along with the common project. This commit
         * replaces the use of the MessageAddedNotification with an anonymous class instead to achieve compatibility between the different
         * versions.
         */

        private void addTransactionListener(CommandContext commandContext) {

            final JobExecutor jobExecutor = Context.getProcessEngineConfiguration().getJobExecutor();
            TransactionContext transactionContext = commandContext.getTransactionContext();
            transactionContext.addTransactionListener(TransactionState.COMMITTED, new TransactionListener() {

                @Override
                public void execute(CommandContext commandContext) {
                    jobExecutor.jobWasAdded();

                }
            });
        }
    }
}
