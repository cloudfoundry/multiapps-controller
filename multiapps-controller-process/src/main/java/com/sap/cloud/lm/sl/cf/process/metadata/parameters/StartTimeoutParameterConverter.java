package com.sap.cloud.lm.sl.cf.process.metadata.parameters;

import org.cloudfoundry.multiapps.common.SLException;

import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.cf.web.api.model.parameters.IntegerParameterConverter;

public class StartTimeoutParameterConverter extends IntegerParameterConverter {

    @Override
    public Integer convert(Object value) {
        int startTimeout = super.convert(value);
        if (startTimeout < 0) {
            throw new SLException(Messages.ERROR_PARAMETER_1_MUST_NOT_BE_NEGATIVE, startTimeout, Variables.START_TIMEOUT.getName());
        }
        return startTimeout;
    }

}
