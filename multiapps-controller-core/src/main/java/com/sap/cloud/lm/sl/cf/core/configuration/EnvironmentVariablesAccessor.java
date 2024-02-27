package com.sap.cloud.lm.sl.cf.core.configuration;

import java.util.Map;

interface EnvironmentVariablesAccessor {

    Map<String, String> getAllVariables();

    String getVariable(String name);

}
