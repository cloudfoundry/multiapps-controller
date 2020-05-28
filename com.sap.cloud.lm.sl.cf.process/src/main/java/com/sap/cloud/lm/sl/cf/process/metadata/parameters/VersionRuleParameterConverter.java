package com.sap.cloud.lm.sl.cf.process.metadata.parameters;

import java.util.Arrays;

import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.cf.web.api.model.parameters.ParameterConverter;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.VersionRule;

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
                                  Messages.ERROR_PARAMETER_1_IS_NOT_VALID_VALID_VALUES_ARE_2,
                                  versionRule,
                                  Variables.VERSION_RULE.getName(),
                                  Arrays.asList(VersionRule.values()));
        }
    }

}
