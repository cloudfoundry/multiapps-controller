package com.sap.cloud.lm.sl.cf.core.helpers;

import com.sap.cloud.lm.sl.common.util.CommonUtil;

// TODO: Use this class (instead of System.getenv()) in ConfigurationUtil, so that it can be tested.
public class Environment {

    public String getVariable(String name) {
        return System.getenv(name);
    }

    public boolean hasVariable(String name) {
        return !CommonUtil.isNullOrEmpty(System.getenv(name));
    }

}
