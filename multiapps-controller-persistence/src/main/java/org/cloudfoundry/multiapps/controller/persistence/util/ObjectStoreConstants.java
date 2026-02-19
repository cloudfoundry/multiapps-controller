package org.cloudfoundry.multiapps.controller.persistence.util;

import java.time.Duration;

public class ObjectStoreConstants {

    private ObjectStoreConstants() {
    }

    public static final int OBJECT_STORE_MAX_ATTEMPTS_CONFIG = 6;
    public static final double OBJECT_STORE_RETRY_DELAY_MULTIPLIER_CONFIG = 2.0;
    public static final Duration OBJECT_STORE_TOTAL_TIMEOUT_CONFIG_IN_MINUTES = Duration.ofMinutes(10);
    public static final Duration OBJECT_STORE_MAX_RETRY_DELAY_CONFIG_IN_SECONDS = Duration.ofSeconds(10);
    public static final Duration OBJECT_STORE_INITIAL_RETRY_DELAY_CONFIG_IN_MILLIS = Duration.ofMillis(250);
}
