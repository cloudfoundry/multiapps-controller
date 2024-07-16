package org.cloudfoundry.multiapps.controller.process.metadata.parameters;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.api.model.parameters.ParameterConverter;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variable;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import java.text.MessageFormat;
import java.time.Duration;

public class TimeoutParameterConverter implements ParameterConverter {

    private final Variable<Duration> timeoutVariable;

    public TimeoutParameterConverter(Variable<Duration> timeoutVariable) {
        this.timeoutVariable = timeoutVariable;
    }

    @Override
    public Duration convert(Object value) {
        int startTimeoutInSeconds = parseToInt(value);
        validateTimeout(startTimeoutInSeconds);
        return Duration.ofSeconds(startTimeoutInSeconds);
    }

    private int parseToInt(Object value) {
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new ContentException(e, MessageFormat.format(Messages.NOT_INTEGER_PARAMETER_VALUE, value, timeoutVariable.getName()));
        }
    }

    private void validateTimeout(int startTimeoutInSeconds) {
        if (startTimeoutInSeconds < 0) {
            throw new ContentException(Messages.ERROR_PARAMETER_1_MUST_NOT_BE_NEGATIVE, startTimeoutInSeconds, timeoutVariable.getName());
        }

        int maxAllowedTimeout = timeoutVariable.equals(Variables.APPS_TASK_EXECUTION_TIMEOUT_COMMAND_LINE_LEVEL) ? 86400 // 24h for task
                                                                                                                         // execution
            : 10800; // 3h for other timeouts

        if (startTimeoutInSeconds > maxAllowedTimeout) {
            throw new ContentException(Messages.ERROR_PARAMETER_1_MUST_BE_LESS_THAN_2,
                                       startTimeoutInSeconds,
                                       timeoutVariable.getName(),
                                       maxAllowedTimeout);
        }
    }
}
