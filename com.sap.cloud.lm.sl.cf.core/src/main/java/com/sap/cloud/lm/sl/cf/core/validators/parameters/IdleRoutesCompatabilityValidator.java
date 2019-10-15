package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import java.util.Arrays;
import java.util.List;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;

public class IdleRoutesCompatabilityValidator implements CompatabilityParameterValidator {

    @Override
    public boolean isCompatible(String parameter) {
        List<String> incompatibleParameters = getIncompatibleParameters();
        return !incompatibleParameters.contains(parameter);
    }

    @Override
    public String getParameterName() {
        return SupportedParameters.IDLE_ROUTES;
    }

    @Override
    public List<String> getIncompatibleParameters() {
        return Arrays.asList(SupportedParameters.IDLE_HOST, SupportedParameters.IDLE_HOSTS, SupportedParameters.IDLE_DOMAIN,
                             SupportedParameters.IDLE_DOMAINS, SupportedParameters.ROUTE_PATH);
    }

}
