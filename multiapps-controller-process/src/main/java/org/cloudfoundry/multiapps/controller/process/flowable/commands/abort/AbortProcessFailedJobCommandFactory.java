package org.cloudfoundry.multiapps.controller.process.flowable.commands.abort;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.engine.impl.jobexecutor.DefaultFailedJobCommandFactory;
import org.springframework.context.annotation.Lazy;

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
