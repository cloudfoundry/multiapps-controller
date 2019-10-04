package com.sap.cloud.lm.sl.cf.process.util;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.cloud.lm.sl.cf.process.util.ElementUpdater.UpdateBehavior;

public abstract class ApplicationAttributeUpdater {

    protected final CloudApplication existingApp;
    protected final StepLogger stepLogger;
    protected final UpdateBehavior updateBehavior;

    public ApplicationAttributeUpdater(CloudApplication existingApp, StepLogger stepLogger) {
        this(existingApp, UpdateBehavior.REPLACE, stepLogger);
    }

    public ApplicationAttributeUpdater(CloudApplication existingApp, UpdateBehavior updateBehavior, StepLogger stepLogger) {
        this.existingApp = existingApp;
        this.stepLogger = stepLogger;
        this.updateBehavior = updateBehavior;
    }

    public UpdateState updateApplication(CloudControllerClient client, CloudApplication app) {
        return shouldUpdateAttribute(app) ? updateApplicationAttribute(client, app) : UpdateState.UNCHANGED;
    }

    protected abstract boolean shouldUpdateAttribute(CloudApplication app);

    protected abstract UpdateState updateApplicationAttribute(CloudControllerClient client, CloudApplication app);

    public enum UpdateState {
        UPDATED, UNCHANGED
    }

}
