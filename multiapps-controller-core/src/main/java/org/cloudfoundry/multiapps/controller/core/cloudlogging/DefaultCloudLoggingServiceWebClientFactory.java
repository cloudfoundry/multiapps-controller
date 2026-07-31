package org.cloudfoundry.multiapps.controller.core.cloudlogging;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Named("cloudLoggingServiceWebClientFactory")
public class DefaultCloudLoggingServiceWebClientFactory implements CloudLoggingServiceWebClientFactory {

    @Override
    public WebClient createWebClientWithMtls(LoggingConfiguration loggingConfiguration) {
        SslContext sslContext = getSslContext(loggingConfiguration);

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
            throw new SLException(e.getMessage());
        }
    }

    private InputStream getCredentialInputStream(String credential) {
        return new ByteArrayInputStream(credential.getBytes(StandardCharsets.UTF_8));
    }
}
