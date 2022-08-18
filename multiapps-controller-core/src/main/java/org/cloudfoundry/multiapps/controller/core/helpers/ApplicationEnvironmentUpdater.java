package org.cloudfoundry.multiapps.controller.core.helpers;

import java.util.Map;
import java.util.TreeMap;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.common.util.MapUtil;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;

public class ApplicationEnvironmentUpdater {

    private final CloudApplication app;
    private final Map<String, String> appEnv;
    private final CloudControllerClient client;
    private boolean prettyPrinting = true;

    public ApplicationEnvironmentUpdater(CloudApplication app, Map<String, String> appEnv, CloudControllerClient client) {
        this.app = app;
        this.appEnv = appEnv;
        this.client = client;
    }

    protected ApplicationEnvironmentUpdater withPrettyPrinting(boolean prettyPrinting) {
        this.prettyPrinting = prettyPrinting;
        return this;
    }

    public void updateApplicationEnvironment(String envPropertyKey, String key, Object value) {
        try {
            Map<String, String> env = new TreeMap<>(appEnv);
            if (envPropertyKey == null) {
                Map<String, Object> updatedEnv = addToEnvironmentProperty(MapUtil.cast(env), key, value);
                updateEnvironment(updatedEnv);
                return;
            }

            String locatedEnvString = env.get(envPropertyKey);
            Map<String, Object> updatedEnv = addToEnvironmentProperty(JsonUtil.convertJsonToMap(locatedEnvString), key, value);
            env.put(envPropertyKey, JsonUtil.toJson(updatedEnv, prettyPrinting));
            updateEnvironment(MapUtil.cast(env));
        } catch (Exception e) {
            throw new SLException(e, "Error updating environment of application");
        }
    }

    private Map<String, Object> addToEnvironmentProperty(Map<String, Object> envProperty, String key, Object value) {
        Map<String, Object> env = new TreeMap<>(envProperty);
        env.put(key, value);
        return env;
    }

    private void updateEnvironment(Map<String, Object> updatedEnv) {
        Map<String, String> asEnv = new MapToEnvironmentConverter(prettyPrinting).asEnv(updatedEnv);
        client.updateApplicationEnv(app.getName(), asEnv);
    }

}
