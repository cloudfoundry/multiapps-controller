package org.cloudfoundry.multiapps.controller.process.util;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;

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
