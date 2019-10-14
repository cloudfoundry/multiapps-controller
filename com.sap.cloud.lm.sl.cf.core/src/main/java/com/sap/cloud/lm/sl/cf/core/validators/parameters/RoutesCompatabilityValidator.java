package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import java.util.Arrays;
import java.util.List;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;

public class RoutesCompatabilityValidator implements CompatabilityParameterValidator {

    @Override
    public boolean isCompatible(String parameter) {
        List<String> incompatibleParameters = getIncompatibleParameters();
        return !incompatibleParameters.contains(parameter);
    }

    @Override
    public String getParameterName() {
        return SupportedParameters.ROUTES;
    }

    @Override
    public List<String> getIncompatibleParameters() {
        return Arrays.asList(SupportedParameters.HOST, SupportedParameters.HOSTS, SupportedParameters.DOMAIN, SupportedParameters.DOMAINS,
                             SupportedParameters.ROUTE_PATH);
    }

}
