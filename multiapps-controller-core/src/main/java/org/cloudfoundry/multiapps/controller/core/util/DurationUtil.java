package org.cloudfoundry.multiapps.controller.core.util;

import java.time.Duration;
import java.util.Optional;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.core.Messages;

public final class DurationUtil {

    public static final int MAX_SERVICE_TIMEOUT_SECONDS = 7200;

    private DurationUtil() {
        // utility class
    }

    public static Duration parseDuration(Object timeout, String parameterName, int maxAllowedValue) {
        if (timeout == null) {
            return null;
        }
        if (!(timeout instanceof Number number)) {
            throw new ContentException(Messages.PARAMETER_0_MUST_BE_A_NUMBER, parameterName);
        }
        int value = number.intValue();
        if (value < 0 || value > maxAllowedValue) {
            throw new ContentException(Messages.PARAMETER_0_MUST_BE_POSITIVE_WITH_MAX_VALUE_1, parameterName, maxAllowedValue);
        }
        return Duration.ofSeconds(value);
    }

    public static Optional<Duration> parseDurationSafely(Object timeout, int maxAllowedValue) {
        if (timeout == null || !(timeout instanceof Number number)) {
            return Optional.empty();
        }
        long seconds = number.longValue();
        return seconds > 0 && seconds <= maxAllowedValue ? Optional.of(Duration.ofSeconds(seconds)) : Optional.empty();
    }
}

