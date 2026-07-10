package org.cloudfoundry.multiapps.controller.core.cloudlogging;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableLoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.LogLevel;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CloudLoggingServiceWebClientFactoryTest {

    private CloudLoggingServiceWebClientFactory factory;

    @BeforeEach
    void setUp() {
        factory = new CloudLoggingServiceWebClientFactory();
    }

    @Test
    void createWebClientWithMtls_failSafeTrue_returnsNullOnInvalidCredentials() {
        LoggingConfiguration config = configBuilder(true).serverCa("not a pem")
                                                         .clientCert("not a pem")
                                                         .clientKey("not a pem")
                                                         .build();

        WebClient webClient = factory.createWebClientWithMtls(config);

        assertNull(webClient);
    }

    @Test
    void createWebClientWithMtls_failSafeFalse_throwsOnInvalidCredentials() {
        LoggingConfiguration config = configBuilder(false).serverCa("not a pem")
                                                          .clientCert("not a pem")
                                                          .clientKey("not a pem")
                                                          .build();

        assertThrows(SLException.class, () -> factory.createWebClientWithMtls(config));
    }

    private static ImmutableLoggingConfiguration.Builder configBuilder(boolean failSafe) {
        return ImmutableLoggingConfiguration.builder()
                                            .operationId("op-1")
                                            .mtaSpaceId("space-1")
                                            .logLevel(LogLevel.INFO)
                                            .isFailSafe(failSafe)
                                            .endpointUrl("https://cls.example.com")
                                            .serverCa("server-ca")
                                            .clientCert("client-cert")
                                            .clientKey("client-key");
    }
}
