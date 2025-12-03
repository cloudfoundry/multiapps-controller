package org.cloudfoundry.multiapps.controller.process.jobs;

import java.text.MessageFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdown;
import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdown.Status;
import org.cloudfoundry.multiapps.controller.persistence.services.ApplicationShutdownService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

@Named
public class ApplicationShutdownJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationShutdownJob.class);

    private final FlowableFacade flowableFacade;
    private final ApplicationShutdownService applicationShutdownService;
    private final ApplicationConfiguration applicationConfiguration;

    //todo add timeout if exeeded just accept the instance has stopped and update status.
    @Inject
    public ApplicationShutdownJob(FlowableFacade flowableFacade, ApplicationShutdownService applicationShutdownService,
                                  ApplicationConfiguration applicationConfiguration) {
        this.flowableFacade = flowableFacade;
        this.applicationShutdownService = applicationShutdownService;
        this.applicationConfiguration = applicationConfiguration;
    }

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.SECONDS)
    public void run() {
        String applicationId = applicationConfiguration.getApplicationGuid();
        int applicationInstanceIndex = applicationConfiguration.getApplicationInstanceIndex();

        ApplicationShutdown applicationShutdown = applicationShutdownService.createQuery()
                                                                            .applicationId(applicationId)
                                                                            .applicationInstanceIndex(applicationInstanceIndex)
                                                                            .singleResult();
        if (applicationShutdown == null || applicationShutdown.getStatus()
                                                              .equals(Status.FINISHED.name())) {
            return;
        }
        Status currentShutdownStatus = getShutdownStatus();
        if (currentShutdownStatus == Status.INITIAL) {
            shutdownApplication(applicationShutdown);
        } else if (currentShutdownStatus == Status.FINISHED) {
            applicationShutdownService.updateApplicationShutdownStatus(applicationShutdown,
                                                                       Status.FINISHED.name());
        }
    }

    private Status getShutdownStatus() {
        return flowableFacade.isJobExecutorActive() ? Status.RUNNING : Status.FINISHED;
    }

    private void shutdownApplication(ApplicationShutdown applicationShutdown) {
        CompletableFuture.runAsync(() -> {
                             logProgressOfShuttingDown(applicationShutdown, Messages.SHUTTING_DOWN_APPLICATION_WITH_ID_AND_INDEX);
                             applicationShutdownService.updateApplicationShutdownStatus(applicationShutdown,
                                                                                        Status.RUNNING.name());
                             flowableFacade.shutdownJobExecutor();
                         })
                         .thenRun(() -> logProgressOfShuttingDown(applicationShutdown, Messages.SHUT_DOWN_APPLICATION_WITH_ID_AND_INDEX));
    }

    private void logProgressOfShuttingDown(ApplicationShutdown applicationShutdown, String message) {
        LOGGER.info(
            MessageFormat.format(message, applicationShutdown.getApplicationId(), applicationShutdown.getApplicationInstanceIndex()));
    }
}
