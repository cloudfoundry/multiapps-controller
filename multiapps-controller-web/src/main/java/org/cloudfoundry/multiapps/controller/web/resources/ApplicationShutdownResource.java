package org.cloudfoundry.multiapps.controller.web.resources;

import jakarta.inject.Inject;
import org.cloudfoundry.multiapps.controller.core.application.shutdown.ApplicationShutdownScheduler;
import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdown;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.cloudfoundry.multiapps.controller.web.Constants;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = Constants.Resources.APPLICATION_SHUTDOWN)
public class ApplicationShutdownResource {

    @Inject
    private FlowableFacade flowableFacade;

    @Inject
    private ApplicationShutdownScheduler applicationShutdownScheduler;

    @PostMapping(produces = { MediaType.APPLICATION_JSON_VALUE })
    public ApplicationShutdown
    shutdownFlowableJobExecutor(@RequestHeader(name = "x-cf-applicationid", required = false) String applicationId,
                                @RequestHeader(name = "x-cf-instanceid", required = false) String applicationInstanceId,
                                @RequestHeader(name = "x-cf-instanceindex", required = false) String applicationInstanceIndex) {

        return applicationShutdownScheduler.scheduleApplicationForShutdown(applicationInstanceId, applicationId, applicationInstanceIndex);
    }

    private ApplicationShutdown.Status getShutdownStatus() {
        return flowableFacade.isJobExecutorActive() ? ApplicationShutdown.Status.RUNNING : ApplicationShutdown.Status.FINISHED;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ApplicationShutdown
    getFlowableJobExecutorShutdownStatus(@RequestHeader(name = "x-cf-applicationid", required = false) String applicationId,
                                         @RequestHeader(name = "x-cf-instanceid", required = false) String applicationInstanceId,
                                         @RequestHeader(name = "x-cf-instanceindex", required = false) String applicationInstanceIndex) {
        return applicationShutdownScheduler.getScheduledApplicationForShutdown(applicationInstanceId, applicationId,
                                                                               applicationInstanceIndex);
    }

}
