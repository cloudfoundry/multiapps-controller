package org.cloudfoundry.multiapps.controller.persistence.services.cloudlogging;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

@Named("cloudLoggingServiceWebClientCache")
public class CloudLoggingServiceWebClientCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudLoggingServiceWebClientCache.class);
    private final Map<String, WebClient> CLIENT_CACHE = Collections.synchronizedMap(new HashMap<>());

    public WebClient getOrCreate(LoggingConfiguration loggingConfiguration,
                                 Function<LoggingConfiguration, WebClient> webClientSupplier) {
        String operationId = loggingConfiguration.getOperationId();
        WebClient cached = CLIENT_CACHE.get(operationId);
        if (cached != null) {
            return cached;
        }
        WebClient created = webClientSupplier.apply(loggingConfiguration);
        if (created != null) {
            CLIENT_CACHE.put(operationId, created);
            LOGGER.debug(MessageFormat.format(Messages.CREATING_WEBCLIENT_WITH_MTLS_CONFIGURATION_FOR_ENDPOINT_1,
                                              loggingConfiguration.getEndpointUrl()));
        }
        return created;
    }

    public void evict(String operationId) {
        CLIENT_CACHE.remove(operationId);
    }
}
