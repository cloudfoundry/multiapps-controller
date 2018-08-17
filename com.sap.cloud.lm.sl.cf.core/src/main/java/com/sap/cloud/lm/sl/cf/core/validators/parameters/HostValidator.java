package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import java.util.Locale;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.v1_0.Module;

public class HostValidator implements ParameterValidator {

    public static final String HOST_ILLEGAL_CHARACTERS = "[^a-z0-9\\-]";
    public static final String HOST_PATTERN = "^([a-z0-9]|[a-z0-9][a-z0-9\\-]{0,61}[a-z0-9])|\\*$";
    public static final int HOST_MAX_LENGTH = 63;

    @Override
    public String attemptToCorrect(Object host) {
        if (!(host instanceof String)) {
            throw new SLException(Messages.COULD_NOT_CREATE_VALID_HOST, host);
        }
        String result = (String) host;
        result = NameUtil.getNameWithProperLength(result, HOST_MAX_LENGTH);
        result = result.toLowerCase(Locale.US);
        result = result.replaceAll(HOST_ILLEGAL_CHARACTERS, "-");
        result = result.replaceAll("^(\\-*)", "");
        result = result.replaceAll("(\\-*)$", "");
        if (!isValid(result)) {
            throw new SLException(Messages.COULD_NOT_CREATE_VALID_HOST, host);
        }
        return result;
    }

    @Override
    public boolean isValid(Object host) {
        if (!(host instanceof String)) {
            return false;
        }
        String hostString = (String) host;
        return !hostString.isEmpty() && NameUtil.isValidName(hostString, HOST_PATTERN);
    }

    @Override
    public String getParameterName() {
        return SupportedParameters.HOST;
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
