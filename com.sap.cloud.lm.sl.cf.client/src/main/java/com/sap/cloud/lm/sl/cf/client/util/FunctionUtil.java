package com.sap.cloud.lm.sl.cf.client.util;

import java.util.concurrent.Callable;

import com.sap.cloud.lm.sl.common.util.Runnable;

public class FunctionUtil {
    public static Callable<Void> callable(Runnable r) {
        return new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                r.run();
                return null;
            }
        };
    }
}
