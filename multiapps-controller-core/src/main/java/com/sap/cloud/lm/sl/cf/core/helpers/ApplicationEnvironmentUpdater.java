package com.sap.cloud.lm.sl.cf.core.helpers;

import static com.sap.cloud.lm.sl.common.util.MapUtil.cast;

import java.util.Map;
import java.util.TreeMap;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

public class ApplicationEnvironmentUpdater {

    private CloudApplication app;
    private CloudControllerClient client;
    private boolean prettyPrinting = true;

    public ApplicationEnvironmentUpdater(CloudApplication app, CloudControllerClient client) {
        this.app = app;
        this.client = client;
    }

    protected ApplicationEnvironmentUpdater withPrettyPrinting(boolean prettyPrinting) {
        this.prettyPrinting = prettyPrinting;
        return this;
    }

    public void updateApplicationEnvironment(String envPropertyKey, String key, Object value) {
        try {
            Map<String, String> appEnvAsMap = app.getEnvAsMap();
            if (envPropertyKey == null) {
                Map<String, Object> updatedEnv = addToEnvironmentProperty(cast(appEnvAsMap), key, value);
                updateEnvironment(updatedEnv);
                return;
            }

            String locatedEnvString = appEnvAsMap.get(envPropertyKey);
            Map<String, Object> updatedEnv = addToEnvironmentProperty(JsonUtil.convertJsonToMap(locatedEnvString), key, value);
            appEnvAsMap.put(envPropertyKey, JsonUtil.toJson(updatedEnv, prettyPrinting));
            updateEnvironment(cast(appEnvAsMap));
        } catch (Exception e) {
            throw new SLException(e, "Error updating environment of application");
        }
    }

    private Map<String, Object> addToEnvironmentProperty(Map<String, Object> envProperty, String key, Object value) {
        Map<String, Object> environment = new TreeMap<>(envProperty);
        environment.put(key, value);
        return environment;
    }

    private void updateEnvironment(Map<String, Object> updatedEnv) {
        Map<String, String> asEnv = new MapToEnvironmentConverter(prettyPrinting).asEnv(updatedEnv);
        client.updateApplicationEnv(app.getName(), asEnv);
    }

}
