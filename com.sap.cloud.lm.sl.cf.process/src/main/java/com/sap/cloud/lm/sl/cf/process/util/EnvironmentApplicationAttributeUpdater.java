package com.sap.cloud.lm.sl.cf.process.util;

import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudApplication;

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
        getLogger().debug("Updating env of application \"{0}\"...", application.getName());
        getLogger().debug("Updated env: {0}", JsonUtil.toJson(application.getEnv(), true));

        Map<String, String> updateEnv = getElementUpdater().updateMap(existingApplication.getEnv(), application.getEnv());
        getControllerClient().updateApplicationEnv(application.getName(), updateEnv);
    }

}
