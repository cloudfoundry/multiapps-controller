package org.cloudfoundry.multiapps.controller.process.metadata.parameters;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.api.model.parameters.IntegerParameterConverter;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

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
