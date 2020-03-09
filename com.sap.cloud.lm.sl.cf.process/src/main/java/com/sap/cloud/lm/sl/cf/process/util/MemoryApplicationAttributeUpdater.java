package com.sap.cloud.lm.sl.cf.process.util;

import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.cloud.lm.sl.cf.process.util.ElementUpdater.UpdateStrategy;

public class MemoryApplicationAttributeUpdater extends ApplicationAttributeUpdater {

    public MemoryApplicationAttributeUpdater(Context context) {
        super(context, UpdateStrategy.REPLACE);
    }

    @Override
    protected boolean shouldUpdateAttribute(CloudApplication existingApplication, CloudApplication application) {
        if (application.getMemory() == 0) { // 0 means "not specified".
            return false;
        }
        return existingApplication.getMemory() != application.getMemory();
    }

    @Override
    protected void updateAttribute(CloudApplication existingApplication, CloudApplication application) {
        getControllerClient().updateApplicationMemory(application.getName(), application.getMemory());
    }

}
