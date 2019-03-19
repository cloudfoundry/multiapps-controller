package com.sap.cloud.lm.sl.cf.process.util;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;

public class DiskQuotaApplicationAttributeUpdater extends ApplicationAttributeUpdater {

    public DiskQuotaApplicationAttributeUpdater(CloudApplication existingApp, StepLogger stepLogger) {
        super(existingApp, stepLogger);
    }

    @Override
    protected boolean shouldUpdateAttribute(CloudApplication app) {
        Integer diskQuota = (app.getDiskQuota() != 0) ? app.getDiskQuota() : null;
        return diskQuota != null && !diskQuota.equals(existingApp.getDiskQuota());
    }

    @Override
    protected UpdateState updateApplicationAttribute(CloudControllerClient client, CloudApplication app) {
        stepLogger.debug("Updating disk quota of application \"{0}\"", app.getName());
        client.updateApplicationDiskQuota(app.getName(), app.getDiskQuota());
        return UpdateState.UPDATED;
    }

}
