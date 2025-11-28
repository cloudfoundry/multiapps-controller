package org.cloudfoundry.multiapps.controller.core.application.shutdown;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdown;
import org.cloudfoundry.multiapps.controller.persistence.dto.ImmutableApplicationShutdown;
import org.cloudfoundry.multiapps.controller.persistence.services.ApplicationShutdownService;

@Named
public class ApplicationShutdownScheduler {

    private final ApplicationShutdownService applicationShutdownService;

    @Inject
    public ApplicationShutdownScheduler(ApplicationShutdownService applicationShutdownService) {
        this.applicationShutdownService = applicationShutdownService;
    }

    public void scheduleApplicationForShutdown(String applicationInstanceId, String applicationId,
                                               int applicationInstanceIndex) {
        ApplicationShutdown a = buildApplicationShutdown(applicationInstanceId, applicationId, applicationInstanceIndex);
        //applicationShutdownService.add();
    }

    private ApplicationShutdown buildApplicationShutdown(String applicationInstanceId, String applicationId,
                                                         int applicationInstanceIndex) {
        return ImmutableApplicationShutdown.builder()
                                           .applicationInstanceId(applicationInstanceId)
                                           .applicationId(applicationId)
                                           .applicationInstanceIndex(applicationInstanceIndex)
                                           .build();
    }
}
