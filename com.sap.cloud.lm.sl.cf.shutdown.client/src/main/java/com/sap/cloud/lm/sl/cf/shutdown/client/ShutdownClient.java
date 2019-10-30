package com.sap.cloud.lm.sl.cf.shutdown.client;

import java.util.UUID;

import com.sap.cloud.lm.sl.cf.core.model.ApplicationShutdown;

public interface ShutdownClient {

    ApplicationShutdown triggerShutdown(UUID applicationGuid, int applicationInstanceIndex);

    ApplicationShutdown getStatus(UUID applicationGuid, int applicationInstanceIndex);

}
