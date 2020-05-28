package com.sap.cloud.lm.sl.cf.web.api.model;

import java.text.MessageFormat;

import com.sap.cloud.lm.sl.common.SLException;

public class IntegerParameterConverter implements ParameterConverter {

    @Override
    public Object convert(Object value) {
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new SLException(e, MessageFormat.format("Parameter value is not integer {0}", value));
        }
    }

}
