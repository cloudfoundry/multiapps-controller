package org.cloudfoundry.multiapps.controller.core.configuration;

import java.util.Map;

public interface EnvironmentVariablesAccessor {

    Map<String, String> getAllVariables();

    String getVariable(String name);

}
