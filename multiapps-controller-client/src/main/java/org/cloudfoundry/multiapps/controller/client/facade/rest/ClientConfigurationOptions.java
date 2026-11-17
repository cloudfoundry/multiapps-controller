package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.time.Duration;
import java.util.Optional;

public record ClientConfigurationOptions(Optional<Duration> connectionTimeout, Optional<Duration> responseTimeout,
                                         Optional<Duration> sslHandshakeTimeout, Optional<Integer> connectionPoolSize,
                                         Optional<Integer> threadPoolSize, boolean trustSelfSignedCertificates) {
}