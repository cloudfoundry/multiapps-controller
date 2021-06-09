package org.cloudfoundry.multiapps.controller.process.metadata.parameters;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.api.model.parameters.ParameterConverter;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import java.text.MessageFormat;
import java.time.Duration;

public class StartTimeoutParameterConverter implements ParameterConverter {

    @Override
    public Duration convert(Object value) {
        int startTimeoutInSeconds;
        try {
            startTimeoutInSeconds = Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new SLException(e, MessageFormat.format("Parameter value is not integer {0}", value));
        }
        if (startTimeoutInSeconds < 0) {
            throw new SLException(Messages.ERROR_PARAMETER_1_MUST_NOT_BE_NEGATIVE, startTimeoutInSeconds, Variables.START_TIMEOUT.getName());
        }
        return Duration.ofSeconds(startTimeoutInSeconds);
    }

}
