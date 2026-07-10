package org.cloudfoundry.multiapps.controller.core.cloudlogging;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.util.CloudLoggingServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Named("cloudLoggingServiceWebClientFactory")
public class CloudLoggingServiceWebClientFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudLoggingServiceWebClientFactory.class);

    public WebClient createWebClientWithMtls(LoggingConfiguration loggingConfiguration) {
        SslContext sslContext = getSslContext(loggingConfiguration);
        if (sslContext == null) {
            return null;
        }
        HttpClient httpClient = HttpClient.create()
                                          .secure(sslSpec -> sslSpec.sslContext(sslContext));

        return WebClient.builder()
                        .baseUrl(loggingConfiguration.getEndpointUrl())
                        .clientConnector(new ReactorClientHttpConnector(httpClient))
                        .build();
    }

    private SslContext getSslContext(LoggingConfiguration loggingConfiguration) {
        try (InputStream serverCaStream = getCredentialInputStream(loggingConfiguration.getServerCa());
            InputStream clientCertStream = getCredentialInputStream(loggingConfiguration.getClientCert());
            InputStream clientKeyStream = getCredentialInputStream(loggingConfiguration.getClientKey())) {
            return SslContextBuilder.forClient()
                                    .keyManager(clientCertStream, clientKeyStream)
                                    .trustManager(serverCaStream)
                                    .build();
        } catch (IOException | IllegalArgumentException e) {
            CloudLoggingServiceUtil.logErrorOrThrowExceptionBasedOnFailSafe(loggingConfiguration, LOGGER, e.getMessage());
            return null;
        }
    }

    private InputStream getCredentialInputStream(String credential) {
        return new ByteArrayInputStream(credential.getBytes(StandardCharsets.UTF_8));
    }
}
