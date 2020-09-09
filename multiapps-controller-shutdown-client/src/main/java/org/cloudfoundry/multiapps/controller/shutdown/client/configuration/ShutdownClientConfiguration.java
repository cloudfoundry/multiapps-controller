package org.cloudfoundry.multiapps.controller.shutdown.client.configuration;

import org.cloudfoundry.multiapps.controller.shutdown.client.ShutdownClient;

/**
 * A small facade around {@link ShutdownConfiguration} that limits what the instances of {@link ShutdownClient} can see from the entire
 * shutdown configuration. They shouldn't need to know the CF API URL, for example.
 * 
 */
public class ShutdownClientConfiguration {

    private final ShutdownConfiguration configuration;

    public ShutdownClientConfiguration(ShutdownConfiguration configuration) {
        this.configuration = configuration;
    }

    public String getApplicationUrl() {
        return configuration.getApplicationUrl();
    }

    public String getUsername() {
        return configuration.getUsername();
    }

    public String getPassword() {
        return configuration.getPassword();
    }

}
