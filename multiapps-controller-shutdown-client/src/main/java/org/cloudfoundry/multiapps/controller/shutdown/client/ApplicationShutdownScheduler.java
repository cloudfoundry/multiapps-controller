package org.cloudfoundry.multiapps.controller.shutdown.client;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdown;
import org.cloudfoundry.multiapps.controller.persistence.dto.ImmutableApplicationShutdown;
import org.cloudfoundry.multiapps.controller.persistence.services.ApplicationShutdownService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationShutdownScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationShutdownScheduler.class);
    private final ApplicationShutdownService applicationShutdownService;

    public ApplicationShutdownScheduler(ApplicationShutdownService applicationShutdownService) {
        this.applicationShutdownService = applicationShutdownService;
    }

    public List<ApplicationShutdown> scheduleApplicationForShutdown(String applicationId, int instancesCount) {
        List<ApplicationShutdown> applicationInstancesForShutdown = new ArrayList<>();
        for (int i = 0; i < instancesCount; i++) {
            ApplicationShutdown applicationShutdown = buildApplicationShutdown(applicationId, i);
            applicationShutdownService.add(applicationShutdown);
            applicationInstancesForShutdown.add(applicationShutdown);
            LOGGER.info(MessageFormat.format(Messages.APP_INSTANCE_WITH_ID_AND_INDEX_SCHEDULED_FOR_SHUTDOWN, applicationId, i));
        }
        return applicationInstancesForShutdown;
    }

    public List<ApplicationShutdown> getScheduledApplicationInstancesForShutdown(String applicationId,
                                                                                 List<String> instancesIds) {
        List<ApplicationShutdown> instances = new ArrayList<>();
        for (String instanceId : instancesIds) {
            ApplicationShutdown applicationShutdown = getApplicationShutdownInstanceByInstanceId(instanceId, applicationId);
            if (applicationShutdown != null) {
                instances.add(applicationShutdown);
            }
        }
        return instances;
    }

    private ApplicationShutdown getApplicationShutdownInstanceByInstanceId(String instanceId, String applicationId) {
        return applicationShutdownService.createQuery()
                                         .id(instanceId)
                                         .applicationId(applicationId)
                                         .singleResult();
    }

    private ApplicationShutdown buildApplicationShutdown(String applicationId,
                                                         int applicationInstanceIndex) {
        return ImmutableApplicationShutdown.builder()
                                           .id(UUID.randomUUID()
                                                   .toString())
                                           .startedAt(LocalDateTime.now())
                                           .applicationId(applicationId)
                                           .applicationInstanceIndex(applicationInstanceIndex)
                                           .build();
    }
}
