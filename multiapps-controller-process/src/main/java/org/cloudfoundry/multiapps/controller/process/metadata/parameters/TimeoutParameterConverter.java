package org.cloudfoundry.multiapps.controller.process.metadata.parameters;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.api.model.parameters.ParameterConverter;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variable;

import java.text.MessageFormat;
import java.time.Duration;

public class TimeoutParameterConverter implements ParameterConverter {

    private final Variable<Duration> timeoutVariable;

    public TimeoutParameterConverter(Variable<Duration> timeoutVariable) {
        this.timeoutVariable = timeoutVariable;
    }

    @Override
    public Duration convert(Object value) {
        int startTimeoutInSeconds;
        try {
            startTimeoutInSeconds = Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new SLException(e, MessageFormat.format(Messages.NOT_INTEGER_PARAMETER_VALUE, value));
        }
        if (startTimeoutInSeconds < 0) {
            throw new SLException(Messages.ERROR_PARAMETER_1_MUST_NOT_BE_NEGATIVE, startTimeoutInSeconds, timeoutVariable.getName());
        }
        return Duration.ofSeconds(startTimeoutInSeconds);
    }

}
