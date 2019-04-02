package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.mta.model.Module;

public class PortValidator implements ParameterValidator {

    public static final int MIN_PORT_VALUE = 1;
    public static final int MAX_PORT_VALUE = 65535;

    @Override
    public boolean isValid(Object port) {
        if (!(port instanceof Integer)) {
            return false;
        }
        return (Integer) port >= MIN_PORT_VALUE && (Integer) port <= MAX_PORT_VALUE;
    }

    @Override
    public Class<?> getContainerType() {
        return Module.class;
    }

    @Override
    public String getParameterName() {
        return SupportedParameters.PORT;
    }

}
