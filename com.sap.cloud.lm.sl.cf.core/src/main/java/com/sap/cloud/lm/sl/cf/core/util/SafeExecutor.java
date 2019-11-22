package com.sap.cloud.lm.sl.cf.core.util;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.common.util.Runnable;

public class SafeExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SafeExecutor.class);

    private final Consumer<Exception> exceptionHandler;

    public SafeExecutor() {
        this(SafeExecutor::log);
    }

    public SafeExecutor(Consumer<Exception> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    public void execute(Runnable runnable) {
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
