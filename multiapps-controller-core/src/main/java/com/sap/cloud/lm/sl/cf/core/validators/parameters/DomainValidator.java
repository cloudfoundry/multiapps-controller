package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import com.sap.cloud.lm.sl.cf.core.Messages;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;

public class DomainValidator extends RoutePartValidator {

    @Override
    public String getParameterName() {
        return SupportedParameters.DOMAIN;
    }

    @Override
    protected String getErrorMessage() {
        return Messages.COULD_NOT_CREATE_VALID_DOMAIN;
    }

    @Override
    protected int getRoutePartMaxLength() {
        return 253;
    }

    @Override
    protected String getRoutePartIllegalCharacters() {
        return "[^a-z-0-9\\-.]";
    }

    @Override
    protected String getRoutePartPattern() {
        return "^([a-z0-9]|[a-z0-9][a-z0-9\\-\\.]{0,251}[a-z0-9])$";
    }

}
