package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import java.util.Locale;
import java.util.Map;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.mta.model.Module;

import com.sap.cloud.lm.sl.cf.core.util.NameUtil;

public abstract class RoutePartValidator implements ParameterValidator {

    @Override
    public String attemptToCorrect(Object routePart, final Map<String, Object> context) {
        if (!(routePart instanceof String)) {
            throw new SLException(getErrorMessage(), routePart);
        }

        String result = (String) routePart;
        result = NameUtil.getNameWithProperLength(result, getRoutePartMaxLength());
        result = result.toLowerCase(Locale.US);
        result = result.replaceAll(getRoutePartIllegalCharacters(), "-");
        result = result.replaceAll("^(-*)", "");
        result = result.replaceAll("(-*)$", "");

        if (!isValid(result, null)) {
            throw new SLException(getErrorMessage(), routePart);
        }
        return result;
    }

    @Override
    public boolean isValid(Object routePart, final Map<String, Object> context) {
        if (!(routePart instanceof String)) {
            return false;
        }

        String part = (String) routePart;
        return !part.isEmpty() && part.matches(getRoutePartPattern());
    }

    @Override
    public Class<?> getContainerType() {
        return Module.class;
    }

    @Override
    public boolean canCorrect() {
        return true;
    }

    protected abstract String getErrorMessage();

    protected abstract int getRoutePartMaxLength();

    protected abstract String getRoutePartIllegalCharacters();

    protected abstract String getRoutePartPattern();
}
