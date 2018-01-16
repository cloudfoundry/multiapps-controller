package com.sap.cloud.lm.sl.cf.core.parser;

import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getPropertyValue;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MemoryParametersParser implements ParametersParser<Integer> {

    private String parameterName;
    private String defaultMemory;

    public MemoryParametersParser(String parameterName, String defaultMemory) {
        this.parameterName = parameterName;
        this.defaultMemory = defaultMemory;
    }

    @Override
    public Integer parse(List<Map<String, Object>> parametersList) {
        return parseMemory((String) getPropertyValue(parametersList, parameterName, defaultMemory));
    }

    protected static int parseMemory(String value) {
        if (getToUpperCase(value).endsWith("M") || getToUpperCase(value).endsWith("MB")) {
            return Integer.parseInt(value.substring(0, getToUpperCase(value).indexOf('M')));
        }
        if (value.toUpperCase().endsWith("G") || getToUpperCase(value).endsWith("GB")) {
            return Integer.parseInt(value.substring(0, getToUpperCase(value).indexOf('G'))) * 1024;
        }
        return Integer.parseInt(value);
    }

    private static String getToUpperCase(String value) {
        return value.toUpperCase(Locale.ROOT);
    }

}
