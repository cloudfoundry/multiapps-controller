package org.cloudfoundry.multiapps.controller.process.util;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudProcess;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.util.ElementUpdater.UpdateStrategy;

public class MemoryApplicationAttributeUpdater extends ApplicationAttributeUpdater {

    private final CloudProcess existingProcess;

    public MemoryApplicationAttributeUpdater(Context context, CloudProcess process) {
        super(context, UpdateStrategy.REPLACE);
        this.existingProcess = process;
    }

    @Override
    protected boolean shouldUpdateAttribute(CloudApplication existingApplication, CloudApplicationExtended application) {
        if (application.getMemory() == 0) { // 0 means "not specified".
            return false;
        }
        return existingProcess.getMemoryInMb() != application.getMemory();
    }

    @Override
    protected void updateAttribute(CloudApplication existingApplication, CloudApplicationExtended application) {
        getControllerClient().updateApplicationMemory(application.getName(), application.getMemory());
    }

}
