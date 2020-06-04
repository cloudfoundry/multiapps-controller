package com.sap.cloud.lm.sl.cf.web.api.model.parameters;

public class StringParameterConverter implements ParameterConverter {

    @Override
    public String convert(Object value) {
        return String.valueOf(value);
    }

}
