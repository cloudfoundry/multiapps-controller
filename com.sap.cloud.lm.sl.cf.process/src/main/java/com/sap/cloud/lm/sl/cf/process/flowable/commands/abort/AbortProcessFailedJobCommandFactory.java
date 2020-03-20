package com.sap.cloud.lm.sl.cf.process.flowable.commands.abort;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.engine.impl.jobexecutor.DefaultFailedJobCommandFactory;

import org.springframework.context.annotation.Lazy;

import javax.inject.Inject;
import javax.inject.Named;

@Named
@Lazy
public class AbortProcessFailedJobCommandFactory extends DefaultFailedJobCommandFactory {

    private AbortProcessFlowableCommandExecutorFactory abortCommandExecutorFactory;

    @Inject
    public AbortProcessFailedJobCommandFactory(AbortProcessFlowableCommandExecutorFactory abortCommandExecutorFactory) {
        this.abortCommandExecutorFactory = abortCommandExecutorFactory;
    }

    @Override
    public Command<Object> getCommand(String jobId, Throwable exception) {
        return new AbortProcessFailedJobCommand(abortCommandExecutorFactory, jobId, super.getCommand(jobId, exception));
    }
}
