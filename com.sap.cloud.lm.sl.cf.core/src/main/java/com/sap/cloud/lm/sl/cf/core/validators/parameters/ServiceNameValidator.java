package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.common.util.MapUtil;
import org.cloudfoundry.multiapps.mta.model.Resource;

import com.sap.cloud.lm.sl.cf.core.Messages;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;

public class ServiceNameValidator implements ParameterValidator {

    private final String namespace;
    private final boolean applyNamespaceGlobal;

    public ServiceNameValidator(String namespace, boolean applyNamespace) {
        this.namespace = namespace;
        this.applyNamespaceGlobal = applyNamespace;
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
    public boolean isValid(Object serviceName, final Map<String, Object> context) {
        // The value supplied by the user must always be corrected.
        return false;
    }

    @Override
    public boolean canCorrect() {
        return true;
    }

    @Override
    public Object attemptToCorrect(Object serviceName, final Map<String, Object> relatedParameters) {
        if (!(serviceName instanceof String)) {
            throw new ContentException(Messages.COULD_NOT_CREATE_VALID_SERVICE_NAME_FROM_0, serviceName);
        }

        boolean applyNamespaceLocal = MapUtil.parseBooleanFlag(relatedParameters, SupportedParameters.APPLY_NAMESPACE, true);
        boolean applyNamespace = applyNamespaceGlobal && applyNamespaceLocal;

        return NameUtil.computeValidServiceName((String) serviceName, namespace, applyNamespace);
    }

    @Override
    public Set<String> getRelatedParameterNames() {
        return Collections.singleton(SupportedParameters.APPLY_NAMESPACE);
    }

}
