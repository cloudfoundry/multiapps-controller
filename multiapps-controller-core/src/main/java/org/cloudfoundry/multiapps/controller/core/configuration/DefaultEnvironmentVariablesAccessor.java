package org.cloudfoundry.multiapps.controller.core.configuration;

import java.util.Map;

class DefaultEnvironmentVariablesAccessor implements EnvironmentVariablesAccessor {

    @Override
    public Map<String, String> getAllVariables() {
        return System.getenv();
    }

    @Override
    public String getVariable(String name) {
        return System.getenv(name);
    }

}
