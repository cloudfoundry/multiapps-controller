package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.util.NameUtil;
import org.cloudfoundry.multiapps.controller.core.util.NamespaceValidationUtil;
import org.cloudfoundry.multiapps.mta.model.Module;

public class ApplicationNameValidator extends NamespaceValidationUtil implements ParameterValidator {

    public ApplicationNameValidator(String namespace, boolean applyNamespaceGlobalLevel, Boolean applyNamespaceProcessVariable) {
        super(namespace, applyNamespaceGlobalLevel, applyNamespaceProcessVariable);
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
    public boolean isValid(Object applicationName, final Map<String, Object> context) {
        // The value supplied by the user must always be corrected.
        return false;
    }

    @Override
    public boolean canCorrect() {
        return true;
    }

    @Override
    public Object attemptToCorrect(Object applicationName, final Map<String, Object> relatedParameters) {
        if (!(applicationName instanceof String)) {
            throw new ContentException(Messages.COULD_NOT_CREATE_VALID_APPLICATION_NAME_FROM_0, applicationName);
        }
        boolean applyNamespaceResult = shouldApplyNamespaceResultValue(relatedParameters);
        return NameUtil.computeValidApplicationName((String) applicationName, getNamespace(), applyNamespaceResult);
    }

    @Override
    public Set<String> getRelatedParameterNames() {
        return Collections.singleton(SupportedParameters.APPLY_NAMESPACE);
    }

}
