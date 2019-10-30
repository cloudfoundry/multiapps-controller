package com.sap.cloud.lm.sl.cf.shutdown.client.configuration;

import java.util.UUID;

public interface ShutdownConfiguration {

    UUID getApplicationGuid();

    String getApplicationUrl();

    String getCloudControllerUrl();

    String getUsername();

    String getPassword();

}
