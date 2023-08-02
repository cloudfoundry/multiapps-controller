package org.cloudfoundry.multiapps.controller.process.util;

import java.util.Map;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ElementUpdater.UpdateStrategy;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;

public class EnvironmentApplicationAttributeUpdater extends ApplicationAttributeUpdater {

    private final Map<String, String> existingAppEnv;

    public EnvironmentApplicationAttributeUpdater(Context context, UpdateStrategy updateStrategy, Map<String, String> appEnv) {
        super(context, updateStrategy);
        this.existingAppEnv = appEnv;
    }

    @Override
    protected boolean shouldUpdateAttribute(CloudApplication existingApplication, CloudApplicationExtended application) {
        Map<String, String> env = application.getEnv();
        return !existingAppEnv.equals(env);
    }

    @Override
    protected void updateAttribute(CloudApplication existingApplication, CloudApplicationExtended application) {
        Map<String, String> env = applyUpdateStrategy(application.getEnv());
        getControllerClient().updateApplicationEnv(application.getName(), env);
    }

    private Map<String, String> applyUpdateStrategy(Map<String, String> env) {
        getLogger().debug(Messages.APPLYING_UPDATE_STRATEGY_0_TO_ENV, updateStrategy);
        return getElementUpdater().updateMap(existingAppEnv, env);
    }

}
