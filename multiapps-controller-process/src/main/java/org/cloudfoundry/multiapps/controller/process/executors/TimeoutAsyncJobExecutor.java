package org.cloudfoundry.multiapps.controller.process.executors;

import org.flowable.job.service.impl.asyncexecutor.DefaultAsyncJobExecutor;

public class TimeoutAsyncJobExecutor extends DefaultAsyncJobExecutor {

    @Override
    protected void initAsyncJobExecutionThreadPool() {
        TimeoutAsyncTaskExecutor taskExecutor = (TimeoutAsyncTaskExecutor) this.taskExecutor;
        taskExecutor.start();
        shutdownTaskExecutor = true;
    }
}
