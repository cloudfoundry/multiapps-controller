package org.cloudfoundry.multiapps.controller.core.cloudlogging;

import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.springframework.web.reactive.function.client.WebClient;

@FunctionalInterface
public interface CloudLoggingServiceWebClientFactory {

    WebClient createWebClientWithMtls(LoggingConfiguration loggingConfiguration);
}
