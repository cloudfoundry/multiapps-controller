package org.cloudfoundry.multiapps.controller.shutdown.client.util;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.List;

import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdown;
import org.cloudfoundry.multiapps.controller.shutdown.client.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShutdownUtil {

    private ShutdownUtil() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ShutdownUtil.class);

    public static final int TIMEOUT_IN_SECONDS = 600; //10 minutes

    public static boolean areThereUnstoppedInstances(List<ApplicationShutdown> shutdownInstances) {
        return shutdownInstances.stream()
                                .anyMatch(shutdownInstance -> !shutdownInstance.getStatus()
                                                                               .equals(ApplicationShutdown.Status.FINISHED));
    }

    public static boolean isTimeoutExceeded(ApplicationShutdown applicationShutdown) {
        LocalDateTime secondsAfterStartedDate = applicationShutdown.getStartedAt()
                                                                   .plusSeconds(TIMEOUT_IN_SECONDS);
        LocalDateTime timeNow = LocalDateTime.now();
        return timeNow.isAfter(secondsAfterStartedDate);
    }

    public static void logShutdownStatus(List<ApplicationShutdown> shutdownInstances) {
        for (ApplicationShutdown shutdownInstance : shutdownInstances) {
            LOGGER.info(
                MessageFormat.format(Messages.SHUTDOWN_STATUS_OF_APPLICATION_WITH_GUID_INSTANCE, shutdownInstance.getApplicationId(),
                                     String.valueOf(shutdownInstance.getApplicationInstanceIndex()), shutdownInstance.getStatus()));
        }
    }
}
