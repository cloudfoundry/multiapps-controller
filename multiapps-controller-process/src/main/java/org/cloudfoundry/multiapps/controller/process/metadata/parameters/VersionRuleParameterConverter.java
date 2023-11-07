package org.cloudfoundry.multiapps.controller.process.metadata.parameters;

import java.util.Arrays;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.api.model.parameters.ParameterConverter;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.VersionRule;

public class VersionRuleParameterConverter implements ParameterConverter {

    @Override
    public Object convert(Object value) {
        String versionRule = String.valueOf(value);
        validate(versionRule);
        return versionRule;
    }

    private void validate(String versionRule) {
        try {
            VersionRule.valueOf(versionRule.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new SLException(e,
                                  Messages.INVALID_VALUE_0_FOR_PARAMETER_1_VALID_VALUES_ARE_2,
                                  versionRule,
                                  Variables.VERSION_RULE.getName(),
                                  Arrays.asList(VersionRule.values()));
        }
    }

}
