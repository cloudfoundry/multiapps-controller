package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.mta.model.Resource;

public class ServiceNameValidator implements ParameterValidator {

    private final String namespace;
    private final boolean useNamespaces;
    private final boolean useNamespacesForServices;

    public ServiceNameValidator(String namespace, boolean useNamespaces, boolean useNamespacesForServices) {
        this.namespace = namespace;
        this.useNamespaces = useNamespaces;
        this.useNamespacesForServices = useNamespacesForServices;
    }

    @Override
    public Class<?> getContainerType() {
        return Resource.class;
    }

    @Override
    public String getParameterName() {
        return SupportedParameters.SERVICE_NAME;
    }

    @Override
    public boolean isValid(Object serviceName) {
        // The value supplied by the user must always be corrected.
        return false;
    }

    @Override
    public boolean canCorrect() {
        return true;
    }

    @Override
    public Object attemptToCorrect(Object serviceName) {
        if (!(serviceName instanceof String)) {
            throw new ContentException(Messages.COULD_NOT_CREATE_VALID_SERVICE_NAME_FROM_0, serviceName);
        }
        return NameUtil.computeValidServiceName((String) serviceName, namespace, useNamespaces, useNamespacesForServices);
    }

}
