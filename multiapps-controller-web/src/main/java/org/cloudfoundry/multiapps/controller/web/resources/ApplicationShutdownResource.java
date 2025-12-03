package org.cloudfoundry.multiapps.controller.web.resources;

import java.util.List;

import jakarta.inject.Inject;
import org.cloudfoundry.multiapps.controller.core.application.shutdown.ApplicationShutdownScheduler;
import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdown;
import org.cloudfoundry.multiapps.controller.web.Constants;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = Constants.Resources.APPLICATION_SHUTDOWN)
public class ApplicationShutdownResource {

    private final ApplicationShutdownScheduler applicationShutdownScheduler;

    @Inject
    public ApplicationShutdownResource(ApplicationShutdownScheduler applicationShutdownScheduler) {
        this.applicationShutdownScheduler = applicationShutdownScheduler;
    }

    @PostMapping(produces = { MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<List<ApplicationShutdown>> shutdownApplicationInstances(
        @RequestParam(name = "applicationId") String applicationId,
        @RequestParam(name = "instancesCount") int instancesCount) {
        return applicationShutdownScheduler.scheduleApplicationForShutdown(applicationId, instancesCount);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ApplicationShutdown>> getApplicationInstancesShutdownStatuses(
        @RequestParam(name = "applicationId") String applicationId) {
        return applicationShutdownScheduler.getScheduledApplicationInstancesForShutdown(applicationId);
    }

}
