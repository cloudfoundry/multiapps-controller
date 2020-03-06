package com.sap.cloud.lm.sl.cf.process.util;

import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ElementUpdater.UpdateStrategy;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

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
        updateApplicationEnv(application.getName(), env);
    }

    private Map<String, String> applyUpdateStrategy(Map<String, String> existingEnv, Map<String, String> env) {
        getLogger().debug(Messages.EXISTING_ENV_0, JsonUtil.toJson(existingEnv, true));
        getLogger().debug(Messages.APPLYING_UPDATE_STRATEGY_0_TO_ENV_1, updateStrategy, JsonUtil.toJson(env, true));
        Map<String, String> result = getElementUpdater().updateMap(existingEnv, env);
        getLogger().debug(Messages.RESULT_0, JsonUtil.toJson(result, true));
        return result;
    }

    private void updateApplicationEnv(String applicationName, Map<String, String> env) {
        getLogger().debug(Messages.UPDATING_ENV_OF_APP_0_TO_1, applicationName, JsonUtil.toJson(env, true));
        getControllerClient().updateApplicationEnv(applicationName, env);
    }

}
