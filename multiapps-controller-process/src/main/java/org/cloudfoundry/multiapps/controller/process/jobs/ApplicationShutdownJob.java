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

    @Inject
    public ApplicationShutdownJob(FlowableFacade flowableFacade, ApplicationShutdownService applicationShutdownService,
                                  ApplicationConfiguration applicationConfiguration) {
        this.flowableFacade = flowableFacade;
        this.applicationShutdownService = applicationShutdownService;
        this.applicationConfiguration = applicationConfiguration;
    }

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.SECONDS)
    public void run() {
        ApplicationShutdown applicationShutdown = getApplicationToShutdown();

        if (applicationShutdown == null || applicationShutdown.getStatus()
                                                              .equals(Status.FINISHED.name())) {
            return;
        }
        if (applicationShutdown.getStatus()
                               .equals(Status.INITIAL.name())) {
            shutdownApplication(applicationShutdown);
            applicationShutdownService.updateApplicationShutdownStatus(applicationShutdown,
                                                                       Status.RUNNING.name());
        } else if (getShutdownStatus() == Status.FINISHED) {
            applicationShutdownService.updateApplicationShutdownStatus(applicationShutdown,
                                                                       Status.FINISHED.name());
        }
    }

    private ApplicationShutdown getApplicationToShutdown() {
        String applicationId = applicationConfiguration.getApplicationGuid();
        int applicationInstanceIndex = applicationConfiguration.getApplicationInstanceIndex();

        return applicationShutdownService.createQuery()
                                         .applicationId(applicationId)
                                         .applicationInstanceIndex(applicationInstanceIndex)
                                         .singleResult();
    }

    private Status getShutdownStatus() {
        return flowableFacade.isJobExecutorActive() ? Status.RUNNING : Status.FINISHED;
    }

    private void shutdownApplication(ApplicationShutdown applicationShutdown) {
        CompletableFuture.runAsync(() -> {
                             logProgressOfShuttingDown(applicationShutdown, Messages.SHUTTING_DOWN_APPLICATION_WITH_ID_AND_INDEX);
                             flowableFacade.shutdownJobExecutor();
                         })
                         .thenRun(() -> logProgressOfShuttingDown(applicationShutdown, Messages.SHUT_DOWN_APPLICATION_WITH_ID_AND_INDEX));
    }

    private void logProgressOfShuttingDown(ApplicationShutdown applicationShutdown, String message) {
        LOGGER.info(
            MessageFormat.format(message, applicationShutdown.getApplicationId(), applicationShutdown.getApplicationInstanceIndex()));
    }
}
