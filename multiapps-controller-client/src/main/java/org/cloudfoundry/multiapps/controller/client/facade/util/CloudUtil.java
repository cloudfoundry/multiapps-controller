package org.cloudfoundry.multiapps.controller.client.facade.util;

import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.Locale;
import java.util.function.Supplier;

import org.cloudfoundry.multiapps.controller.client.facade.Messages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some helper utilities used by the Cloud Foundry Java client.
 *
 */
public class CloudUtil {

    private static final Double DEFAULT_DOUBLE = 0.0;
    private static final Integer DEFAULT_INTEGER = 0;
    private static final Long DEFAULT_LONG = 0L;
    private static final int RETRIES = 3;
    private static final Duration RETRY_INTERVAL = Duration.ofSeconds(3);
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudUtil.class);

    private CloudUtil() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T parse(Class<T> clazz, Object object) {
        T defaultValue = null;
        try {
            if (clazz == Date.class) {
                String stringValue = parse(String.class, object);
                return clazz.cast(new SimpleDateFormat("EEE MMM d HH:mm:ss Z yyyy", Locale.US).parse(stringValue));
            }

            if (clazz == Integer.class) {
                defaultValue = (T) DEFAULT_INTEGER;
            } else if (clazz == Long.class) {
                defaultValue = (T) DEFAULT_LONG;
            } else if (clazz == Double.class) {
                defaultValue = (T) DEFAULT_DOUBLE;
            }

            if (object == null) {
                return defaultValue;
            }

            // special handling for int and long since smaller numbers become ints
            // but may be requested as long and vice versa
            if (clazz == Integer.class) {
                if (object instanceof Number) {
                    return clazz.cast(((Number) object).intValue());
                } else if (object instanceof String) {
                    return clazz.cast(Integer.valueOf(((String) object)));
                }
            }
            if (clazz == Long.class) {
                if (object instanceof Number) {
                    return clazz.cast(((Number) object).longValue());
                } else if (object instanceof String) {
                    return clazz.cast(Long.valueOf(((String) object)));
                }
            }
            if (clazz == Double.class) {
                if (object instanceof Number) {
                    return clazz.cast(((Number) object).doubleValue());
                } else if (object instanceof String) {
                    return clazz.cast(Double.valueOf(((String) object)));
                }
            }

            return clazz.cast(object);
        } catch (ClassCastException | ParseException e) {
            // ignore
        }
        return defaultValue;
    }

    public static <T> T executeWithRetry(Supplier<T> operation) {
        for (int i = 1; i < RETRIES; i++) {
            try {
                return operation.get();
            } catch (Exception e) {
                LOGGER.warn(MessageFormat.format(Messages.RETRYING_OPERATION, e.getMessage()), e);
                sleep(RETRY_INTERVAL);
            }
        }
        return operation.get();
    }

    public static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted!", e);
        }
    }

}
