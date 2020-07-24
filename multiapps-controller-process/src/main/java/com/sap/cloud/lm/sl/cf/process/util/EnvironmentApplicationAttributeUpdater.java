package com.sap.cloud.lm.sl.cf.process.util;

import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ElementUpdater.UpdateStrategy;

public class EnvironmentApplicationAttributeUpdater extends ApplicationAttributeUpdater {

    public EnvironmentApplicationAttributeUpdater(Context context, UpdateStrategy updateStrategy) {
        super(context, updateStrategy);
    }

    @Override
    protected boolean shouldUpdateAttribute(CloudApplication existingApplication, CloudApplication application) {
        Map<String, String> env = application.getEnv();
        Map<String, String> existingEnv = existingApplication.getEnv();
        return !existingEnv.equals(env);
    }

    @Override
    protected void updateAttribute(CloudApplication existingApplication, CloudApplication application) {
        Map<String, String> env = applyUpdateStrategy(existingApplication.getEnv(), application.getEnv());
        getControllerClient().updateApplicationEnv(application.getName(), env);
    }

    private Map<String, String> applyUpdateStrategy(Map<String, String> existingEnv, Map<String, String> env) {
        getLogger().debug(Messages.APPLYING_UPDATE_STRATEGY_0_TO_ENV, updateStrategy);
        return getElementUpdater().updateMap(existingEnv, env);
    }

}
