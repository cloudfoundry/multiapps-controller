package com.sap.cloud.lm.sl.cf.process.util;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;

public class MemoryApplicationAttributeUpdater extends ApplicationAttributeUpdater {

    public MemoryApplicationAttributeUpdater(CloudApplication existingApp, StepLogger stepLogger) {
        super(existingApp, stepLogger);
    }

    @Override
    protected boolean shouldUpdateAttribute(CloudApplication app) {
        Integer applicationMemory = (app.getMemory() != 0) ? app.getMemory() : null;
        return applicationMemory != null && !applicationMemory.equals(existingApp.getMemory());
    }

    @Override
    protected UpdateState updateApplicationAttribute(CloudControllerClient client, CloudApplication app) {
        stepLogger.debug("Updating memory of application \"{0}\"", app.getName());
        client.updateApplicationMemory(app.getName(), app.getMemory());
        return UpdateState.UPDATED;
    }

}
