package org.cloudfoundry.multiapps.controller.shutdown.client.util;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.List;

import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdown;
import org.cloudfoundry.multiapps.controller.shutdown.client.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShutdownUtil {

    private ShutdownUtil() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ShutdownUtil.class);

    private static final int TIMEOUT_IN_SECONDS = 600; //10 minutes

    public static boolean areThereUnstoppedInstances(List<ApplicationShutdown> shutdown) {
        return shutdown.stream()
                       .anyMatch(applicationShutdown -> !applicationShutdown.getStatus()
                                                                            .equals(ApplicationShutdown.Status.FINISHED.name()));
    }

    public static boolean isTimeoutExceeded(ApplicationShutdown applicationShutdown) {
        Instant thirtyMinutesAfterStartedDate = Instant.from(applicationShutdown.getStaredAt()
                                                                                .toInstant())
                                                       .plusSeconds(TIMEOUT_IN_SECONDS);
        Instant timeNow = Instant.now();
        return timeNow.isAfter(thirtyMinutesAfterStartedDate);
    }

    public static void print(List<ApplicationShutdown> shutdown) {
        for (ApplicationShutdown applicationShutdown : shutdown) {
            LOGGER.info(
                MessageFormat.format(Messages.SHUTDOWN_STATUS_OF_APPLICATION_WITH_GUID_INSTANCE, applicationShutdown.getApplicationId(),
                                     String.valueOf(applicationShutdown.getApplicationInstanceIndex()), applicationShutdown.getStatus()));
        }
    }
}
