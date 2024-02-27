package com.sap.cloud.lm.sl.cf.web.api.model;

import java.text.MessageFormat;

import com.sap.cloud.lm.sl.common.SLException;

public interface ParameterTypeProvider {
    Object getParameterType(Object value);
}

class IntegerParameterTypeProvider implements ParameterTypeProvider {

    @Override
    public Object getParameterType(Object value) {
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new SLException(e, MessageFormat.format("Parameter value is not integer {0}", value));
        }
    }

}

class StringParameterTypeProvider implements ParameterTypeProvider {

    @Override
    public Object getParameterType(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}

class BooleanParameterTypeProvider implements ParameterTypeProvider {

    @Override
    public Object getParameterType(Object value) {
        String parameterValue = String.valueOf(value);
        if ("true".equals(parameterValue)) {
            return true;
        } else if ("false".equals(parameterValue)) {
            return false;
        } else {
            throw new SLException(MessageFormat.format("Parameter is not boolean {0}", value));
        }
    }

}
