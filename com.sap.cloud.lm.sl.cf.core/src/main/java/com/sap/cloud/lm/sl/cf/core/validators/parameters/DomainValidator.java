package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import java.util.Locale;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.Messages;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.mta.model.Module;

public class DomainValidator implements ParameterValidator {

    public static final String DOMAIN_ILLEGAL_CHARACTERS = "[^a-z-0-9\\-.]";
    public static final String DOMAIN_PATTERN = "^([a-z0-9]|[a-z0-9][a-z0-9\\-\\.]{0,251}[a-z0-9])$";
    public static final int DOMAIN_MAX_LENGTH = 253;

    @Override
    public String attemptToCorrect(Object domain, final Map<String, Object> context) {
        String result = (String) domain;
        result = NameUtil.getNameWithProperLength(result, DOMAIN_MAX_LENGTH);
        result = result.toLowerCase(Locale.US);
        result = result.replaceAll(DOMAIN_ILLEGAL_CHARACTERS, "-");
        result = result.replaceAll("^(-*)", "");
        result = result.replaceAll("(-*)$", "");
        if (!isValid(result, null)) {
            throw new ContentException(Messages.COULD_NOT_CREATE_VALID_DOMAIN, domain);
        }
        return result;
    }

    @Override
    public boolean isValid(Object domain, final Map<String, Object> context) {

        if (!(domain instanceof String)) {
            return false;
        }
        String domainString = (String) domain;
        return !domainString.isEmpty() && NameUtil.isValidName(domainString, DOMAIN_PATTERN);
    }

    @Override
    public String getParameterName() {
        return SupportedParameters.DOMAIN;
    }

    @Override
    public Class<?> getContainerType() {
        return Module.class;
    }

    @Override
    public boolean canCorrect() {
        return true;
    }

}
