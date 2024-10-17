package org.cloudfoundry.multiapps.controller.process.metadata.parameters;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.api.model.parameters.ParameterConverter;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.TimeoutType;
import org.cloudfoundry.multiapps.controller.process.variables.Variable;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import java.text.MessageFormat;
import java.time.Duration;

public class TimeoutParameterConverter implements ParameterConverter {

    private static final int MAX_TIMEOUT = TimeoutType.START.getMaxAllowedValue();
    private static final int MAX_TASK_EXECUTION_TIMEOUT = TimeoutType.TASK.getMaxAllowedValue();
    private final Variable<Duration> timeoutVariable;

    public TimeoutParameterConverter(Variable<Duration> timeoutVariable) {
        this.timeoutVariable = timeoutVariable;
    }

    @Override
    public Duration convert(Object value) {
        int timeoutInSeconds = parseToInt(value);
        validateTimeout(timeoutInSeconds);
        return Duration.ofSeconds(timeoutInSeconds);
    }

    private int parseToInt(Object value) {
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new ContentException(e, MessageFormat.format(Messages.NOT_INTEGER_PARAMETER_VALUE, value, timeoutVariable.getName()));
        }
    }

    private void validateTimeout(int timeoutInSeconds) {
        if (timeoutInSeconds < 0) {
            throw new ContentException(Messages.ERROR_PARAMETER_1_MUST_NOT_BE_NEGATIVE, timeoutInSeconds, timeoutVariable.getName());
        }

        int maxAllowedTimeout = timeoutVariable.equals(Variables.APPS_TASK_EXECUTION_TIMEOUT_PROCESS_VARIABLE) ? MAX_TASK_EXECUTION_TIMEOUT
            : MAX_TIMEOUT;

        if (timeoutInSeconds > maxAllowedTimeout) {
            throw new ContentException(Messages.ERROR_PARAMETER_1_MUST_BE_LESS_THAN_2,
                                       timeoutInSeconds,
                                       timeoutVariable.getName(),
                                       maxAllowedTimeout);
        }
    }
}
