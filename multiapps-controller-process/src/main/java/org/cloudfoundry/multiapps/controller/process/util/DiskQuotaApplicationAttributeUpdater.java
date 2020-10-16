package org.cloudfoundry.multiapps.controller.process.util;

import org.cloudfoundry.multiapps.controller.process.util.ElementUpdater.UpdateStrategy;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;

public class DiskQuotaApplicationAttributeUpdater extends ApplicationAttributeUpdater {

    public DiskQuotaApplicationAttributeUpdater(Context context) {
        super(context, UpdateStrategy.REPLACE);
    }

    @Override
    protected boolean shouldUpdateAttribute(CloudApplication existingApplication, CloudApplication application) {
        if (application.getDiskQuota() == 0) { // 0 means "not specified".
            return false;
        }
        return existingApplication.getDiskQuota() != application.getDiskQuota();
    }

    @Override
    protected void updateAttribute(CloudApplication existingApplication, CloudApplication application) {
        getControllerClient().updateApplicationDiskQuota(application.getName(), application.getDiskQuota());
    }

}
