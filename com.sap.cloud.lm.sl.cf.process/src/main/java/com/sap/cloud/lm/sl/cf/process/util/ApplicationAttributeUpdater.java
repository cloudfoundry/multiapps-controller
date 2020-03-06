package com.sap.cloud.lm.sl.cf.process.util;

import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.cloud.lm.sl.cf.process.util.ElementUpdater.UpdateStrategy;

public abstract class ApplicationAttributeUpdater extends ControllerClientFacade {

    public enum UpdateState {
        UPDATED, UNCHANGED
    }

    protected final UpdateStrategy strategy;

    public ApplicationAttributeUpdater(Context context, UpdateStrategy strategy) {
        super(context);
        this.strategy = strategy;
    }

    public UpdateState update(CloudApplication existingApplication, CloudApplication application) {
        if (!shouldUpdateAttribute(existingApplication, application)) {
            return UpdateState.UNCHANGED;
        }
        updateAttribute(existingApplication, application);
        return UpdateState.UPDATED;
    }

    protected abstract boolean shouldUpdateAttribute(CloudApplication existingApplication, CloudApplication application);

    protected abstract void updateAttribute(CloudApplication existingApplication, CloudApplication application);

    protected final ElementUpdater getElementUpdater() {
        return ElementUpdater.getUpdater(strategy);
    }

}
