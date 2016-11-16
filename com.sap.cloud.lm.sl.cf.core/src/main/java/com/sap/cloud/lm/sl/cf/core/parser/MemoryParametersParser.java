package com.sap.cloud.lm.sl.cf.core.parser;

import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getPropertyValue;

import java.util.List;
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
        if (value.toUpperCase().endsWith("M") || value.toUpperCase().endsWith("MB")) {
            return Integer.parseInt(value.substring(0, value.toUpperCase().indexOf('M')));
        }
        if (value.toUpperCase().endsWith("G") || value.toUpperCase().endsWith("GB")) {
            return Integer.parseInt(value.substring(0, value.toUpperCase().indexOf('G'))) * 1024;
        }
        return Integer.parseInt(value);
    }

}
