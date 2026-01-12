package org.cloudfoundry.multiapps.controller.shutdown.client;

import java.util.List;

import jakarta.persistence.EntityManagerFactory;
import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdown;
import org.cloudfoundry.multiapps.controller.persistence.services.ApplicationShutdownService;
import org.cloudfoundry.multiapps.controller.shutdown.client.configuration.DatabaseConnector;
import org.cloudfoundry.multiapps.controller.shutdown.client.util.ShutdownUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationShutdownExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationShutdownExecutor.class);
    private static final long SHUTDOWN_POLLING_INTERVAL = 5000L;

    public static void main(String[] args) {
        String applicationId = args[0];
        int applicationInstanceCount = Integer.parseInt(args[1]);
        new ApplicationShutdownExecutor().execute(applicationId, applicationInstanceCount);
    }

    public void execute(String applicationId, int applicationInstanceCount) {
        ApplicationShutdownScheduler applicationShutdownScheduler = getApplicationShutdownScheduler();
        List<ApplicationShutdown> scheduledApplicationShutdowns = applicationShutdownScheduler.scheduleApplicationForShutdown(applicationId,
                                                                                                                              applicationInstanceCount);
        List<String> applicationShutdownInstancesIds = getApplicationShutdownInstancesIds(scheduledApplicationShutdowns);
        List<ApplicationShutdown> applicationShutdowns = applicationShutdownScheduler.getScheduledApplicationInstancesForShutdown(
            applicationId, applicationShutdownInstancesIds);

        while (ShutdownUtil.areThereUnstoppedInstances(applicationShutdowns) && !ShutdownUtil.isTimeoutExceeded(
            applicationShutdowns.get(0))) {
            ShutdownUtil.logShutdownStatus(applicationShutdowns);
            MiscUtil.sleep(SHUTDOWN_POLLING_INTERVAL);
            applicationShutdowns = applicationShutdownScheduler.getScheduledApplicationInstancesForShutdown(
                applicationId, applicationShutdownInstancesIds);
        }
        LOGGER.info(Messages.FINISHED_SHUTTING_DOWN);
    }

    public List<String> getApplicationShutdownInstancesIds(List<ApplicationShutdown> applicationShutdowns) {
        return applicationShutdowns.stream()
                                   .map(ApplicationShutdown::getId)
                                   .toList();
    }

    public ApplicationShutdownScheduler getApplicationShutdownScheduler() {
        DatabaseConnector databaseConnector = new DatabaseConnector();
        EntityManagerFactory entityManagerFactory = databaseConnector.createEntityManagerFactory();
        ApplicationShutdownService.ApplicationShutdownMapper applicationShutdownMapper = new ApplicationShutdownService.ApplicationShutdownMapper();
        ApplicationShutdownService applicationShutdownService = new ApplicationShutdownService(entityManagerFactory,
                                                                                               applicationShutdownMapper);

        return new ApplicationShutdownScheduler(applicationShutdownService);
    }
}
