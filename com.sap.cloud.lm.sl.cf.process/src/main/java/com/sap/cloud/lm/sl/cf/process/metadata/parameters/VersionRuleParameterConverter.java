package com.sap.cloud.lm.sl.cf.process.metadata.parameters;

import java.util.Arrays;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.mta.model.VersionRule;

import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.cf.web.api.model.parameters.ParameterConverter;

public class VersionRuleParameterConverter implements ParameterConverter {

    @Override
    public Object convert(Object value) {
        String versionRule = String.valueOf(value);
        validate(versionRule);
        return versionRule;
    }

    private void validate(String versionRule) {
        try {
            VersionRule.value(versionRule);
        } catch (IllegalArgumentException e) {
            throw new SLException(e,
                                  Messages.INVALID_VALUE_0_FOR_PARAMETER_1_VALID_VALUES_ARE_2,
                                  versionRule,
                                  Variables.VERSION_RULE.getName(),
                                  Arrays.asList(VersionRule.values()));
        }
    }

}
