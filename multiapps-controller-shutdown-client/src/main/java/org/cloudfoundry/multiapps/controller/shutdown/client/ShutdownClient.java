package org.cloudfoundry.multiapps.controller.shutdown.client;

import java.util.UUID;

import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdown;

public interface ShutdownClient {

    ApplicationShutdown triggerShutdown(UUID applicationGuid, int applicationInstanceIndex);

    ApplicationShutdown getStatus(UUID applicationGuid, int applicationInstanceIndex);

}
