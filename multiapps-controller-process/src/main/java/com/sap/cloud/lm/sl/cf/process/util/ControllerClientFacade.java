package com.sap.cloud.lm.sl.cf.process.util;

import org.cloudfoundry.client.lib.CloudControllerClient;

public abstract class ControllerClientFacade {

    public static class Context {

        private final CloudControllerClient controllerClient;
        private final StepLogger logger;

        public Context(CloudControllerClient controllerClient, StepLogger logger) {
            this.controllerClient = controllerClient;
            this.logger = logger;
        }

    }

    private final Context context;

    ControllerClientFacade(Context context) {
        this.context = context;
    }

    final CloudControllerClient getControllerClient() {
        return context.controllerClient;
    }

    final StepLogger getLogger() {
        return context.logger;
    }

}
