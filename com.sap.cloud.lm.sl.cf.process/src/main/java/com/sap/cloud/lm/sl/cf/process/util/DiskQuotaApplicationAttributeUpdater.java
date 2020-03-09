package com.sap.cloud.lm.sl.cf.process.util;

import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.cloud.lm.sl.cf.process.util.ElementUpdater.UpdateStrategy;

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
