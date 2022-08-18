package org.cloudfoundry.multiapps.controller.process.util;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.util.ElementUpdater.UpdateStrategy;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudProcess;

public class DiskQuotaApplicationAttributeUpdater extends ApplicationAttributeUpdater {

    private final CloudProcess existingProcess;

    public DiskQuotaApplicationAttributeUpdater(Context context, CloudProcess process) {
        super(context, UpdateStrategy.REPLACE);
        this.existingProcess = process;
    }

    @Override
    protected boolean shouldUpdateAttribute(CloudApplication existingApplication, CloudApplicationExtended application) {
        if (application.getDiskQuota() == 0) { // 0 means "not specified".
            return false;
        }
        return existingProcess.getDiskInMb() != application.getDiskQuota();
    }

    @Override
    protected void updateAttribute(CloudApplication existingApplication, CloudApplicationExtended application) {
        getControllerClient().updateApplicationDiskQuota(application.getName(), application.getDiskQuota());
    }

}
