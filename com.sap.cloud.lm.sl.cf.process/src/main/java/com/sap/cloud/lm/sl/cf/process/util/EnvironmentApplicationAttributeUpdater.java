package com.sap.cloud.lm.sl.cf.process.util;

import java.util.Map;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.cloud.lm.sl.cf.process.util.ElementUpdater.UpdateBehavior;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;

public class EnvironmentApplicationAttributeUpdater extends ApplicationAttributeUpdater {

    public EnvironmentApplicationAttributeUpdater(CloudApplication existingApp, UpdateBehavior updateBehavior, StepLogger stepLogger) {
        super(existingApp, updateBehavior, stepLogger);
    }

    @Override
    protected boolean shouldUpdateAttribute(CloudApplication app) {
        return !app.getEnvAsMap()
            .equals(existingApp.getEnvAsMap());
    }

    @Override
    protected UpdateState updateApplicationAttribute(CloudControllerClient client, CloudApplication app) {
        stepLogger.debug("Updating env of application \"{0}\"", app.getName());
        stepLogger.debug("Updated env: {0}", JsonUtil.toJson(app.getEnv(), true));

        Map<String, String> updateEnv = ElementUpdater.getUpdater(updateBehavior)
            .updateMap(existingApp.getEnvAsMap(), app.getEnvAsMap());
        app.setEnv(MapUtil.upcastUnmodifiable(updateEnv));
        client.updateApplicationEnv(app.getName(), app.getEnv());

        return UpdateState.UPDATED;
    }

}
