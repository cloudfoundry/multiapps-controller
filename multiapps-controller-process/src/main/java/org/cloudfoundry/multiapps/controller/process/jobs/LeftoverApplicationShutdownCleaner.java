package org.cloudfoundry.multiapps.controller.process.jobs;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdown;
import org.cloudfoundry.multiapps.controller.persistence.services.ApplicationShutdownService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.shutdown.client.util.ShutdownUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class LeftoverApplicationShutdownCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(LeftoverApplicationShutdownCleaner.class);

    private final ApplicationShutdownService applicationShutdownService;

    public LeftoverApplicationShutdownCleaner(ApplicationShutdownService applicationShutdownService) {
        this.applicationShutdownService = applicationShutdownService;
    }

    @Override
    public void execute(LocalDateTime expirationTime) {
        List<ApplicationShutdown> leftoverApplicationShutdowns = getApplicationShutdownsScheduledForMoreThanADay();

        if (leftoverApplicationShutdowns.isEmpty()) {
            LOGGER.info(Messages.NO_LEFTOVER_APPLICATION_SHUTDOWNS);
            return;
        }
        
        deleteLeftoverApplicationShutdowns(leftoverApplicationShutdowns);
        LOGGER.info(MessageFormat.format(Messages.DELETED_LEFTOVER_APPLICATION_SHUTDOWNS, leftoverApplicationShutdowns.size()));
    }

    private List<ApplicationShutdown> getApplicationShutdownsScheduledForMoreThanADay() {
        List<ApplicationShutdown> applicationShutdowns = applicationShutdownService.createQuery()
                                                                                   .list();
        return applicationShutdowns.stream()
                                   .filter(ShutdownUtil::isApplicationShutdownScheduledForMoreThanADay)
                                   .toList();
    }

    private void deleteLeftoverApplicationShutdowns(List<ApplicationShutdown> leftoverApplicationShutdowns) {
        leftoverApplicationShutdowns.forEach(applicationShutdown -> applicationShutdownService.createQuery()
                                                                                              .id(applicationShutdown.getId())
                                                                                              .delete());
    }
}
