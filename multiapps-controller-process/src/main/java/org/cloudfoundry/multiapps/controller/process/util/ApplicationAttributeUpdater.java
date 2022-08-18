package org.cloudfoundry.multiapps.controller.process.util;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.util.ElementUpdater.UpdateStrategy;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;

public abstract class ApplicationAttributeUpdater extends ControllerClientFacade {

    public enum UpdateState {
        UPDATED, UNCHANGED
    }

    protected final UpdateStrategy updateStrategy;

    protected ApplicationAttributeUpdater(Context context, UpdateStrategy updateStrategy) {
        super(context);
        this.updateStrategy = updateStrategy;
    }

    public UpdateState update(CloudApplication existingApplication, CloudApplicationExtended application) {
        if (!shouldUpdateAttribute(existingApplication, application)) {
            return UpdateState.UNCHANGED;
        }
        updateAttribute(existingApplication, application);
        return UpdateState.UPDATED;
    }

    protected abstract boolean shouldUpdateAttribute(CloudApplication existingApplication, CloudApplicationExtended application);

    protected abstract void updateAttribute(CloudApplication existingApplication, CloudApplicationExtended application);

    protected final ElementUpdater getElementUpdater() {
        return ElementUpdater.getUpdater(updateStrategy);
    }

}
