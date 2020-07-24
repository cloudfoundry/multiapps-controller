package com.sap.cloud.lm.sl.cf.process.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.cloud.lm.sl.cf.process.Messages;
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
        List<String> uris = applyUpdateStrategy(existingApplication.getUris(), application.getUris());
        getControllerClient().updateApplicationUris(application.getName(), uris);
    }

    private List<String> applyUpdateStrategy(List<String> existingUris, List<String> uris) {
        getLogger().debug(Messages.EXISTING_URIS_0, uris);
        getLogger().debug(Messages.APPLYING_UPDATE_STRATEGY_0_TO_URIS_1, updateStrategy, uris);
        List<String> result = getElementUpdater().updateList(existingUris, uris);
        getLogger().debug(Messages.RESULT_0, result);
        return result;
    }

}
