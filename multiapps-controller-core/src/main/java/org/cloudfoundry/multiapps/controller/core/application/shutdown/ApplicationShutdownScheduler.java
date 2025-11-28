package org.cloudfoundry.multiapps.controller.core.application.shutdown;

import java.text.MessageFormat;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdown;
import org.cloudfoundry.multiapps.controller.persistence.dto.ImmutableApplicationShutdown;
import org.cloudfoundry.multiapps.controller.persistence.services.ApplicationShutdownService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ApplicationShutdownScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationShutdownScheduler.class);
    private final ApplicationShutdownService applicationShutdownService;

    @Inject
    public ApplicationShutdownScheduler(ApplicationShutdownService applicationShutdownService) {
        this.applicationShutdownService = applicationShutdownService;
    }

    public ApplicationShutdown scheduleApplicationForShutdown(String applicationInstanceId, String applicationId,
                                                              String applicationInstanceIndex) {
        LOGGER.info(MessageFormat.format(Messages.APP_INSTANCE_WITH_ID_AND_INDEX_SCHEDULED_FOR_DELETION, applicationInstanceId,
                                         applicationInstanceIndex));
        ApplicationShutdown applicationShutdown = buildApplicationShutdown(applicationInstanceId, applicationId,
                                                                           Integer.parseInt(applicationInstanceIndex));
        return applicationShutdownService.add(applicationShutdown);
    }

    public ApplicationShutdown getScheduledApplicationForShutdown(String applicationInstanceId, String applicationId,
                                                                  String applicationInstanceIndex) {
        ApplicationShutdown applicationToShutdown = applicationShutdownService.createQuery()
                                                                              .applicationId(applicationId)
                                                                              .applicationInstanceIndex(
                                                                                  Integer.parseInt(applicationInstanceIndex))
                                                                              .applicationInstanceId(applicationInstanceId)
                                                                              .singleResult();

        LOGGER.info(MessageFormat.format(Messages.APP_SHUTDOWN_STATUS_MONITOR, applicationId, applicationInstanceId,
                                         applicationInstanceIndex, applicationToShutdown.getStatus()));

        return applicationToShutdown;

    }

    public ApplicationShutdown getScheduledApplicationForShutdownByIndex(int applicationInstanceIndex) {
        return applicationShutdownService.createQuery()
                                         .applicationInstanceIndex(applicationInstanceIndex)
                                         .singleResult();
    }

    public void deleteScheduledApplication(int applicationInstanceIndex) {
        applicationShutdownService.createQuery()
                                  .applicationInstanceIndex(applicationInstanceIndex)
                                  .delete();
    }

    private ApplicationShutdown buildApplicationShutdown(String applicationInstanceId, String applicationId,
                                                         int applicationInstanceIndex) {
        return ImmutableApplicationShutdown.builder()
                                           .applicationInstanceId(applicationInstanceId)
                                           .applicationId(applicationId)
                                           .applicationInstanceIndex(applicationInstanceIndex)
                                           .status(ApplicationShutdown.Status.RUNNING)
                                           .build();
    }
}
