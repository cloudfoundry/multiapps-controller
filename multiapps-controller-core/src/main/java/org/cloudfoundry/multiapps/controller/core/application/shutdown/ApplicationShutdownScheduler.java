package org.cloudfoundry.multiapps.controller.core.application.shutdown;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdown;
import org.cloudfoundry.multiapps.controller.persistence.dto.ImmutableApplicationShutdown;
import org.cloudfoundry.multiapps.controller.persistence.services.ApplicationShutdownService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

@Named
public class ApplicationShutdownScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationShutdownScheduler.class);
    private final ApplicationShutdownService applicationShutdownService;

    @Inject
    public ApplicationShutdownScheduler(ApplicationShutdownService applicationShutdownService) {
        this.applicationShutdownService = applicationShutdownService;
    }

    public ResponseEntity<List<ApplicationShutdown>> scheduleApplicationForShutdown(String applicationId, int instancesCount) {
        List<ApplicationShutdown> applicationInstancesForShutdown = new ArrayList<>();
        for (int i = 0; i < instancesCount; i++) {
            LOGGER.info(MessageFormat.format(Messages.APP_INSTANCE_WITH_ID_AND_INDEX_SCHEDULED_FOR_DELETION, applicationId, i));
            ApplicationShutdown applicationShutdown = buildApplicationShutdown(applicationId, i);
            applicationShutdownService.add(applicationShutdown);
            applicationInstancesForShutdown.add(applicationShutdown);
        }
        return ResponseEntity.ok()
                             .body(applicationInstancesForShutdown);
    }

    public ResponseEntity<List<ApplicationShutdown>> getScheduledApplicationInstancesForShutdown(String applicationId) {
        List<ApplicationShutdown> applicationToShutdown = applicationShutdownService.getApplicationsByApplicationId(applicationId);

        LOGGER.info(MessageFormat.format(Messages.APP_SHUTDOWN_STATUS_MONITOR, applicationId));

        return ResponseEntity.ok()
                             .body(applicationToShutdown);
    }

    private ApplicationShutdown buildApplicationShutdown(String applicationId,
                                                         int applicationInstanceIndex) {
        return ImmutableApplicationShutdown.builder()
                                           .id(UUID.randomUUID()
                                                   .toString())
                                           .staredAt(Date.from(Instant.now()))
                                           .applicationId(applicationId)
                                           .applicationInstanceIndex(applicationInstanceIndex)
                                           .build();
    }
}
