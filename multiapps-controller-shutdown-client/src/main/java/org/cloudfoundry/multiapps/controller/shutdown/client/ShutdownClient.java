package org.cloudfoundry.multiapps.controller.shutdown.client;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdown;

public interface ShutdownClient {

    List<ApplicationShutdown> triggerShutdown(UUID applicationGuid, int applicationInstancesCount);

    List<ApplicationShutdown> getStatus(UUID applicationGuid);

}
