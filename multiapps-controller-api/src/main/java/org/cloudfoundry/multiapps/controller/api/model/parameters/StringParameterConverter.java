package org.cloudfoundry.multiapps.controller.api.model.parameters;

public class StringParameterConverter implements ParameterConverter {

    @Override
    public String convert(Object value) {
        return String.valueOf(value);
    }

}
