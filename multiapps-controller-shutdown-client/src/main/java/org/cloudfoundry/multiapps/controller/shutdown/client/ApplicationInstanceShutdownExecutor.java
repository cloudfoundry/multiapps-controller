package org.cloudfoundry.multiapps.controller.shutdown.client;

import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdown;
import org.cloudfoundry.multiapps.controller.shutdown.client.configuration.ShutdownClientConfiguration;
import org.cloudfoundry.multiapps.controller.shutdown.client.configuration.ShutdownConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationInstanceShutdownExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationInstanceShutdownExecutor.class);

    private static final long SHUTDOWN_POLLING_INTERVAL = 3000L;

    private final ShutdownConfiguration shutdownConfiguration;
    private final ShutdownClientFactory shutdownClientFactory;

    public ApplicationInstanceShutdownExecutor(ShutdownConfiguration shutdownConfiguration, ShutdownClientFactory shutdownClientFactory) {
        this.shutdownConfiguration = shutdownConfiguration;
        this.shutdownClientFactory = shutdownClientFactory;
    }

    public void execute(UUID applicationGuid, int applicationInstanceIndex) {
        ShutdownClient shutdownClient = createShutdownClient();
        shutdown(shutdownClient, applicationGuid, applicationInstanceIndex);
    }

    private ShutdownClient createShutdownClient() {
        return shutdownClientFactory.createShutdownClient(createShutdownClientConfiguration());
    }

    private ShutdownClientConfiguration createShutdownClientConfiguration() {
        return new ShutdownClientConfiguration(shutdownConfiguration);
    }

    private static void shutdown(ShutdownClient shutdownClient, UUID applicationGuid, int applicationInstanceIndex) {
        List<ApplicationShutdown> shutdown = shutdownClient.triggerShutdown(applicationGuid, applicationInstanceIndex);
        while (!hasFinished(shutdown)) {
            print(shutdown);
            MiscUtil.sleep(SHUTDOWN_POLLING_INTERVAL);
            shutdown = shutdownClient.getStatus(applicationGuid);
        }
    }

    private static boolean hasFinished(List<ApplicationShutdown> shutdown) {
        return shutdown.stream()
                       .anyMatch(applicationShutdown -> !applicationShutdown.getStatus()
                                                                            .equals(ApplicationShutdown.Status.FINISHED.name()));
    }

    private static void print(List<ApplicationShutdown> shutdown) {
        for (ApplicationShutdown applicationShutdown : shutdown) {
            System.out.println(
                MessageFormat.format("Shutdown status of application with GUID {}, instance {}: {}", applicationShutdown.getApplicationId(),
                                     applicationShutdown.getApplicationInstanceIndex(), applicationShutdown.getStatus()));
            LOGGER.info("Shutdown status of application with GUID {}, instance {}: {}", applicationShutdown.getApplicationId(),
                        applicationShutdown.getApplicationInstanceIndex(), applicationShutdown.getStatus());
        }
    }

}
