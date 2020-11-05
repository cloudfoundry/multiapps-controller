package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import java.util.Locale;
import java.util.Map;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.util.NameUtil;
import org.cloudfoundry.multiapps.mta.model.Module;

public abstract class RoutePartValidator implements ParameterValidator {

    @Override
    public String attemptToCorrect(Object routePart, final Map<String, Object> context) {
        if (!(routePart instanceof String)) {
            throw new SLException(getErrorMessage(), routePart);
        }

        String result = ((String) routePart).toLowerCase(Locale.US);
        result = result.replaceAll(getRoutePartIllegalCharacters(), "-");
        result = result.replaceAll("^(-*)", "");
        result = result.replaceAll("(-*)$", "");
        result = modifyAndShortenRoutePart(result, context);

        if (!isValid(result, context)) {
            throw new SLException(getErrorMessage(), routePart);
        }
        return result;
    }

    protected String modifyAndShortenRoutePart(String routePart, Map<String, Object> context) {
        return NameUtil.getNameWithProperLength(routePart, getRoutePartMaxLength());
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
