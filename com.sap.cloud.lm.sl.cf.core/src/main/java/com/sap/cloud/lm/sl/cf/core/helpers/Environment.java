package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.common.util.CommonUtil;

@Component
public class Environment {

    public String getVariable(String name) {
        return System.getenv(name);
    }

    public Map<String, String> getVariables() {
        return System.getenv();
    }

    public boolean hasVariable(String name) {
        return !CommonUtil.isNullOrEmpty(System.getenv(name));
    }

}
