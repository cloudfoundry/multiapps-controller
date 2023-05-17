package org.cloudfoundry.multiapps.controller.core.resolvers.v3;

import java.text.MessageFormat;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.helpers.DynamicResolvableParametersHelper;
import org.cloudfoundry.multiapps.controller.core.model.DynamicResolvableParameter;
import org.cloudfoundry.multiapps.mta.helpers.SimplePropertyVisitor;
import org.cloudfoundry.multiapps.mta.util.DynamicParameterUtil;

public class DynamicParametersResolver implements SimplePropertyVisitor {

    private final String resourceName;
    private final DynamicResolvableParametersHelper dynamicResolvableParametersHelper;

    public DynamicParametersResolver(String resourceName, DynamicResolvableParametersHelper dynamicResolvableParametersHelper) {
        this.resourceName = resourceName;
        this.dynamicResolvableParametersHelper = dynamicResolvableParametersHelper;
    }

    @Override
    public Object visit(String key, String value) {
        if (value.matches(DynamicParameterUtil.REGEX_PATTERN_FOR_DYNAMIC_PARAMETERS)) {
            String relationshipName = DynamicParameterUtil.getRelationshipName(value);
            String parameterName = DynamicParameterUtil.getParameterName(value);
            DynamicResolvableParameter dynamicResolvableParameter = dynamicResolvableParametersHelper.findDynamicResolvableParameter(parameterName,
                                                                                                                                    relationshipName);
            if (dynamicResolvableParameter != null && dynamicResolvableParameter.getValue() != null) {
                return dynamicResolvableParameter.getValue();
            }
            throw new ContentException(MessageFormat.format(Messages.RESOURCE_0_CANNOT_BE_CREATED_DUE_TO_UNRESOLVED_DYNAMIC_PARAMETER,
                                                            resourceName, relationshipName));
        }
        return value;
    }

}
