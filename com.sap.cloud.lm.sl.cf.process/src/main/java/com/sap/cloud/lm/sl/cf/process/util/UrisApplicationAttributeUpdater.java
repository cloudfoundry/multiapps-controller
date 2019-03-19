package com.sap.cloud.lm.sl.cf.process.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.cloud.lm.sl.cf.process.util.ElementUpdater.UpdateBehavior;

public class UrisApplicationAttributeUpdater extends ApplicationAttributeUpdater {

    public UrisApplicationAttributeUpdater(CloudApplication existingApp, UpdateBehavior updateBehavior, StepLogger stepLogger) {
        super(existingApp, updateBehavior, stepLogger);
    }

    @Override
    protected boolean shouldUpdateAttribute(CloudApplication app) {
        Set<String> urisSet = new HashSet<>(app.getUris());
        Set<String> existingUrisSet = new HashSet<>(existingApp.getUris());
        return !urisSet.equals(existingUrisSet);
    }

    @Override
    protected UpdateState updateApplicationAttribute(CloudControllerClient client, CloudApplication app) {
        stepLogger.debug("Updating uris of application \"{0}\" with uri: {1}", app.getName(), app.getUris());
        List<String> updatedUris = ElementUpdater.getUpdater(updateBehavior)
            .updateList(existingApp.getUris(), app.getUris());
        client.updateApplicationUris(app.getName(), updatedUris);
        return UpdateState.UPDATED;
    }

}
