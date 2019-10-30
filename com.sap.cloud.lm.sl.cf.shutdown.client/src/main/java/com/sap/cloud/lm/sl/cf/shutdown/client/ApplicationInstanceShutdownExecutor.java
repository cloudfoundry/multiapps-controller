package com.sap.cloud.lm.sl.cf.shutdown.client;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.model.ApplicationShutdown;
import com.sap.cloud.lm.sl.cf.shutdown.client.configuration.ShutdownClientConfiguration;
import com.sap.cloud.lm.sl.cf.shutdown.client.configuration.ShutdownConfiguration;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

public class ApplicationInstanceShutdownExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationShutdownExecutor.class);

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
        for (ApplicationShutdown shutdown = shutdownClient.triggerShutdown(applicationGuid,
                                                                           applicationInstanceIndex); !hasFinished(shutdown);) {
            print(shutdown);
            sleep(SHUTDOWN_POLLING_INTERVAL);
            shutdown = shutdownClient.getStatus(applicationGuid, applicationInstanceIndex);
        }
    }

    private static boolean hasFinished(ApplicationShutdown shutdown) {
        return ApplicationShutdown.Status.FINISHED.equals(shutdown.getStatus());
    }

    private static void print(ApplicationShutdown shutdown) {
        LOGGER.info("Shutdown status of application with GUID {}, instance {}: {}", shutdown.getApplicationId(),
                    shutdown.getApplicationInstanceIndex(), JsonUtil.toJson(shutdown, true));
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

}
