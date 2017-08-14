package com.sap.cloud.lm.sl.cf.core.helpers;

// TODO: Use this class (instead of System.getenv()) in ConfigurationUtil, so that it can be tested.
public class Environment {

    public String getVariable(String name) {
        return System.getenv(name);
    }

}
