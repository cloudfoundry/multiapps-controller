package com.sap.cloud.lm.sl.cf.core.util;

import java.util.function.Consumer;

import org.apache.commons.lang3.Functions.FailableRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SafeExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SafeExecutor.class);

    private final Consumer<Exception> exceptionHandler;

    public SafeExecutor() {
        this(SafeExecutor::log);
    }

    public SafeExecutor(Consumer<Exception> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    public <E extends Exception> void execute(FailableRunnable<E> runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            exceptionHandler.accept(e);
        }
    }

    private static void log(Exception e) {
        LOGGER.warn(e.getMessage(), e);
    }

}
