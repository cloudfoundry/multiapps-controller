package com.sap.cloud.lm.sl.cf.process.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.cloud.lm.sl.cf.process.util.ElementUpdater.UpdateStrategy;

public class UrisApplicationAttributeUpdater extends ApplicationAttributeUpdater {

    public UrisApplicationAttributeUpdater(Context context, UpdateStrategy updateStrategy) {
        super(context, updateStrategy);
    }

    @Override
    protected boolean shouldUpdateAttribute(CloudApplication existingApplication, CloudApplication application) {
        Set<String> uris = new HashSet<>(application.getUris());
        Set<String> existingUris = new HashSet<>(existingApplication.getUris());
        return !uris.equals(existingUris);
    }

    @Override
    protected void updateAttribute(CloudApplication existingApplication, CloudApplication application) {
        getLogger().debug("Updating URIs of application \"{0}\" to: {1}", application.getName(), application.getUris());
        List<String> updatedUris = getElementUpdater().updateList(existingApplication.getUris(), application.getUris());
        getControllerClient().updateApplicationUris(application.getName(), updatedUris);
    }

}
