package com.sap.cloud.lm.sl.cf.core.configuration;

import java.util.Map;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.common.util.CommonUtil;

@Component
public class Environment {

    private final EnvironmentVariablesAccessor environmentVariablesAccessor;

    public Environment() {
        this(new DefaultEnvironmentVariablesAccessor());
    }

    public Environment(EnvironmentVariablesAccessor environmentVariablesAccessor) {
        this.environmentVariablesAccessor = environmentVariablesAccessor;
    }

    public Map<String, String> getAllVariables() {
        return environmentVariablesAccessor.getAllVariables();
    }

    public Long getLong(String name) {
        return getLong(name, null);
    }

    public String getString(String name) {
        return getString(name, null);
    }

    public Integer getInteger(String name) {
        return getInteger(name, null);
    }

    public Boolean getBoolean(String name) {
        return getBoolean(name, null);
    }

    public Long getLong(String name, Long defaultValue) {
        try {
            return getVariable(name, Long::valueOf, defaultValue);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public String getString(String name, String defaultValue) {
        return getVariable(name, String::valueOf, defaultValue);
    }

    public Integer getInteger(String name, Integer defaultValue) {
        try {
            return getVariable(name, Integer::valueOf, defaultValue);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public Boolean getBoolean(String name, Boolean defaultValue) {
        return getVariable(name, Boolean::valueOf, defaultValue);
    }

    public Integer getPositiveInteger(String name, Integer defaultValue) {
        Integer value = getInteger(name, defaultValue);
        if (value == null || value <= 0) {
            value = Integer.MAX_VALUE;
        }
        return value;
    }

    public Integer getNegativeInteger(String name, Integer defaultValue) {
        Integer value = getInteger(name, defaultValue);
        if (value == null || value >= 0) {
            value = Integer.MIN_VALUE;
        }
        return value;
    }

    public <T> T getVariable(String name, Function<String, T> mappingFunction) {
        return getVariable(name, mappingFunction, null);
    }

    public <T> T getVariable(String name, Function<String, T> mappingFunction, T defaultValue) {
        String value = environmentVariablesAccessor.getVariable(name);
        return value == null ? defaultValue : mappingFunction.apply(value);
    }

    public boolean hasVariable(String name) {
        return !CommonUtil.isNullOrEmpty(getString(name));
    }

}
