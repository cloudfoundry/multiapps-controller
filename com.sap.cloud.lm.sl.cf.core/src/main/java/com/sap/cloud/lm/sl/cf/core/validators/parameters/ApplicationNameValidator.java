package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.mta.model.v2.Module;

public class ApplicationNameValidator implements ParameterValidator {

    private String namespace;
    private boolean useNamespaces;

    public ApplicationNameValidator(String namespace, boolean useNamespaces) {
        this.namespace = namespace;
        this.useNamespaces = useNamespaces;
    }

    @Override
    public Class<?> getContainerType() {
        return Module.class;
    }

    @Override
    public String getParameterName() {
        return SupportedParameters.APP_NAME;
    }

    @Override
    public boolean isValid(Object applicationName) {
        // The value supplied by the user must always be corrected.
        return false;
    }

    @Override
    public boolean canCorrect() {
        return true;
    }

    @Override
    public Object attemptToCorrect(Object applicationName) {
        if (!(applicationName instanceof String)) {
            throw new ContentException(Messages.COULD_NOT_CREATE_VALID_APPLICATION_NAME_FROM_0, applicationName);
        }
        return NameUtil.getApplicationName((String) applicationName, namespace, useNamespaces);
    }

}
