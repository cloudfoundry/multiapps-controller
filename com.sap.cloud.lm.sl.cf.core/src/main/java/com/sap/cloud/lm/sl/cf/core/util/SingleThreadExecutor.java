package com.sap.cloud.lm.sl.cf.core.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Named;

@Named
public class SingleThreadExecutor {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void submitTask(Runnable task) {
        executor.submit(task);
    }
}
