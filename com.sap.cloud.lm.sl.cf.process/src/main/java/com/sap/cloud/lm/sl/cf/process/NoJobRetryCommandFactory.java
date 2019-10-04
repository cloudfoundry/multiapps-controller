package com.sap.cloud.lm.sl.cf.process;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.jobexecutor.DefaultFailedJobCommandFactory;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sets number of retries on failed Flowable job. By default Flowable will retry failed jobs three times. Responsible also for set of
 * exception and stack trace if available.
 */
public class NoJobRetryCommandFactory extends DefaultFailedJobCommandFactory {

    protected static final Logger LOGGER = LoggerFactory.getLogger(NoJobRetryCommandFactory.class);

    @Override
    public Command<Object> getCommand(String jobId, Throwable exception) {
        return new NoJobRetryCommand(jobId, exception, super.getCommand(jobId, exception));
    }

    public static class NoJobRetryCommand implements Command<Object> {

        private static final int NO_RETRIES = 0;
        private String jobId;
        private Throwable exception;
        private Command<Object> delegate;

        public NoJobRetryCommand(String jobId, Throwable exception, Command<Object> delegate) {
            this.jobId = jobId;
            this.exception = exception;
            this.delegate = delegate;
        }

        @Override
        public Object execute(CommandContext commandContext) {
            JobEntity job = CommandContextUtil.getJobServiceConfiguration(commandContext)
                                              .getJobService()
                                              .findJobById(jobId);
            job.setRetries(NO_RETRIES);
            job.setLockOwner(null);
            job.setLockExpirationTime(null);

            if (exception != null) {
                job.setExceptionMessage(exception.getMessage());
                job.setExceptionStacktrace(getExceptionStacktrace());
            }

            return delegate.execute(commandContext);
        }

        private String getExceptionStacktrace() {
            return ExceptionUtils.getStackTrace(exception);
        }

    }
}
