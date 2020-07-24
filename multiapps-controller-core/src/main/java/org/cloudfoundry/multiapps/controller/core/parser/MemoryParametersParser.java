package org.cloudfoundry.multiapps.controller.core.parser;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.mta.util.PropertiesUtil;

public class MemoryParametersParser implements ParametersParser<Integer> {

    private static final String GIGABYTES_MATCHER = "[0-9]+[Gg][Bb]?";
    private static final String MEGABYTES_MATCHER = "[0-9]+([Mm][Bb]?|$)";
    private final String parameterName;
    private final String defaultMemory;

    public MemoryParametersParser(String parameterName, String defaultMemory) {
        this.parameterName = parameterName;
        this.defaultMemory = defaultMemory;
    }

    @Override
    public Integer parse(List<Map<String, Object>> parametersList) {
        return parseMemory((String) PropertiesUtil.getPropertyValue(parametersList, parameterName, defaultMemory));
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
