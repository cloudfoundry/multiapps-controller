package com.sap.cloud.lm.sl.cf.core.helpers;

import static com.sap.cloud.lm.sl.common.util.MapUtil.cast;

import java.util.*;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

public class ApplicationEnvironmentUpdater {

    private final CloudApplication app;
    private final CloudControllerClient client;
    private final Collection<String> sensitiveVariables;
    private boolean prettyPrinting = true;

    public ApplicationEnvironmentUpdater(CloudApplication app, CloudControllerClient client) {
        this.app = app;
        this.client = client;
        if( app instanceof CloudApplicationExtended ){
            this.sensitiveVariables = ((CloudApplicationExtended) app).getSensitiveEnvVariableNames();
        }else {
            this.sensitiveVariables = Collections.emptyList();
        }
    }

    protected ApplicationEnvironmentUpdater withPrettyPrinting(boolean prettyPrinting) {
        this.prettyPrinting = prettyPrinting;
        return this;
    }

    public void updateApplicationEnvironment(String envPropertyKey, String key, Object value) {
        try {
            Map<String, String> env = new TreeMap<>(app.getEnv());
            if (envPropertyKey == null) {
                Map<String, Object> updatedEnv = addToEnvironmentProperty(cast(env), key, value);
                updateEnvironment(updatedEnv);
                return;
            }

            String locatedEnvString = env.get(envPropertyKey);
            Map<String, Object> updatedEnv = addToEnvironmentProperty(JsonUtil.convertJsonToMap(locatedEnvString), key, value);
            env.put(envPropertyKey, JsonUtil.toJson(updatedEnv, prettyPrinting));
            updateEnvironment(cast(env));
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
        Map<String, String> env = new MapToEnvironmentConverter(prettyPrinting).asEnv(updatedEnv);
        //TODO do the same here so the environment can be printed inside the client.
        client.updateApplicationEnv(app.getName(), env, sensitiveVariables);
    }

}
