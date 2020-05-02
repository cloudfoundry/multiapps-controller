package com.sap.cloud.lm.sl.cf.process.util;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ElementUpdater.UpdateStrategy;
import org.cloudfoundry.client.lib.domain.CloudApplication;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

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
        Collection<String> sensitiveVariables = getSensitiveVariables(application);
        Map<String, String> env = applyUpdateStrategy(existingApplication.getEnv(), application.getEnv(), sensitiveVariables);
        getControllerClient().updateApplicationEnv(application.getName(), env, sensitiveVariables);
    }

    private Map<String, String> applyUpdateStrategy(Map<String, String> existingEnv, Map<String, String> env, Collection<String> sensitiveVariables) {
        SecureSerializationFacade secureSerializer = new SecureSerializationFacade().addSensitiveElementNames(sensitiveVariables)
                                                                                    .setFormattedOutput(true);
        getLogger().debug(Messages.EXISTING_ENV_0, secureSerializer.toJson(existingEnv));
        getLogger().debug(Messages.APPLYING_UPDATE_STRATEGY_0_TO_ENV_1, updateStrategy, secureSerializer.toJson(env));
        Map<String, String> result = getElementUpdater().updateMap(existingEnv, env);
        getLogger().debug(Messages.RESULT_0, secureSerializer.toJson(result));
        return result;
    }

    private Collection<String> getSensitiveVariables(CloudApplication application) {
        if(application instanceof CloudApplicationExtended){
            return ((CloudApplicationExtended) application).getSensitiveEnvVariableNames();
        }
        return Collections.emptyList();
    }
}
