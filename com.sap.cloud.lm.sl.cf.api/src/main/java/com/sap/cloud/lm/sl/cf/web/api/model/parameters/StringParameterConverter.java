package com.sap.cloud.lm.sl.cf.web.api.model.parameters;

public class StringParameterConverter implements ParameterConverter {

    @Override
    public Object convert(Object value) {
        return value == null ? null : String.valueOf(value);
    }

}
