package org.cloudfoundry.multiapps.controller.web.monitoring;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

@Named
public class AppUploaderThreadPoolInformation {

    private final ThreadPoolExecutor appUploaderPool;

    @Inject
    public AppUploaderThreadPoolInformation(
        @Named("appUploaderThreadPool") ExecutorService appUploaderThreadPool) {
        this.appUploaderPool = (ThreadPoolExecutor) appUploaderThreadPool;
    }

    public int getActiveThreads() {
        return appUploaderPool.getActiveCount();
    }

    public int getMaxThreads() {
        return appUploaderPool.getMaximumPoolSize();
    }

}
