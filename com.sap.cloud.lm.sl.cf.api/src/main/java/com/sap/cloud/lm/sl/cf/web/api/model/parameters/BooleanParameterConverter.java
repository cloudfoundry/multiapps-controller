package com.sap.cloud.lm.sl.cf.web.api.model.parameters;

import java.text.MessageFormat;

import com.sap.cloud.lm.sl.common.SLException;

public class BooleanParameterConverter implements ParameterConverter {

    @Override
    public Boolean convert(Object value) {
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
