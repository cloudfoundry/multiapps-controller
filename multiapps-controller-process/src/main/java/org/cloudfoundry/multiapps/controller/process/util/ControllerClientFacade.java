package org.cloudfoundry.multiapps.controller.process.util;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;

public abstract class ControllerClientFacade {

    public static class Context {

        private final CloudControllerClient controllerClient;
        private final ProcessContext processContext;
        private final StepLogger logger;

        public Context(CloudControllerClient controllerClient, ProcessContext processContext, StepLogger logger) {
            this.controllerClient = controllerClient;
            this.processContext = processContext;
            this.logger = logger;
        }

    }

    private final Context context;

    ControllerClientFacade(Context context) {
        this.context = context;
    }

    public final CloudControllerClient getControllerClient() {
        return context.controllerClient;
    }

    public final StepLogger getLogger() {
        return context.logger;
    }

    protected final ProcessContext getProcessContext() {
        return context.processContext;
    }

}
