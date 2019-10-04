package com.sap.cloud.lm.sl.cf.core.parser;

import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getPropertyValue;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.common.ContentException;

public class MemoryParametersParser implements ParametersParser<Integer> {

    private static final String GIGABYTES_MATCHER = "[0-9]+[Gg][Bb]?";
    private static final String MEGABYTES_MATCHER = "[0-9]+([Mm][Bb]?|$)";
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

    public static Integer parseMemory(String value) {
        if (value == null) {
            return null;
        }
        if (value.matches(MEGABYTES_MATCHER)) {
            return getNumberFromString(value);
        }
        if (value.matches(GIGABYTES_MATCHER)) {
            return getNumberFromString(value) * 1024;
        }
        throw new ContentException(MessageFormat.format(Messages.UNABLE_TO_PARSE_MEMORY_STRING_0, value));
    }

    private static int getNumberFromString(String value) {
        return Integer.parseInt(value.replaceAll("[^0-9]", ""));
    }

}
