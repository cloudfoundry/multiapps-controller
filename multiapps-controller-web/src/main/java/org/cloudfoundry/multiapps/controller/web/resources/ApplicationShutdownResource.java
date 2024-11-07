package org.cloudfoundry.multiapps.controller.web.resources;

import java.text.MessageFormat;
import java.util.concurrent.CompletableFuture;

import jakarta.inject.Inject;

import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.model.ApplicationShutdown;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableApplicationShutdown;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/admin/shutdown")
public class ApplicationShutdownResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationShutdownResource.class);

    @Inject
    private FlowableFacade flowableFacade;

    @PostMapping(produces = { MediaType.APPLICATION_JSON_VALUE })
    public ApplicationShutdown
           shutdownFlowableJobExecutor(@RequestHeader(name = "x-cf-applicationid", required = false) String applicationId,
                                       @RequestHeader(name = "x-cf-instanceid", required = false) String applicationInstanceId,
                                       @RequestHeader(name = "x-cf-instanceindex", required = false) String applicationInstanceIndex) {

        CompletableFuture.runAsync(() -> {
            LOGGER.info(MessageFormat.format(Messages.APP_SHUTDOWN_REQUEST, applicationId, applicationInstanceId,
                                             applicationInstanceIndex));
            flowableFacade.shutdownJobExecutor();
        })
                         .thenRun(() -> LOGGER.info(MessageFormat.format(Messages.APP_SHUTDOWNED, applicationId, applicationInstanceId,
                                                                         applicationInstanceIndex)));

        return ImmutableApplicationShutdown.builder()
                                           .status(getShutdownStatus())
                                           .applicationInstanceIndex(Integer.parseInt(applicationInstanceIndex))
                                           .applicationId(applicationId)
                                           .applicationInstanceId(applicationInstanceId)
                                           .build();
    }

    private ApplicationShutdown.Status getShutdownStatus() {
        return flowableFacade.isJobExecutorActive() ? ApplicationShutdown.Status.RUNNING : ApplicationShutdown.Status.FINISHED;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ApplicationShutdown
           getFlowableJobExecutorShutdownStatus(@RequestHeader(name = "x-cf-applicationid", required = false) String applicationId,
                                                @RequestHeader(name = "x-cf-instanceid", required = false) String applicationInstanceId,
                                                @RequestHeader(name = "x-cf-instanceindex", required = false) String applicationInstanceIndex) {

        ApplicationShutdown applicationShutdown = ImmutableApplicationShutdown.builder()
                                                                              .status(getShutdownStatus())
                                                                              .applicationInstanceIndex(Integer.parseInt(applicationInstanceIndex))
                                                                              .applicationId(applicationId)
                                                                              .applicationInstanceId(applicationInstanceId)
                                                                              .build();

        LOGGER.info(MessageFormat.format(Messages.APP_SHUTDOWN_STATUS_MONITOR, applicationId, applicationInstanceId,
                                         applicationInstanceIndex, applicationShutdown.getStatus()));

        return applicationShutdown;
    }

}
