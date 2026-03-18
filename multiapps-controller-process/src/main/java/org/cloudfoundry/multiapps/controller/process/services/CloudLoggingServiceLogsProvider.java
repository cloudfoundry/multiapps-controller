package org.cloudfoundry.multiapps.controller.process.services;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.net.ssl.SSLException;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.core.model.ExternalLoggingServiceConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.ExternalOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableExternalOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.OperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLogsPersistenceService;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Named("cloudLoggingServiceLogsProvider")
public class CloudLoggingServiceLogsProvider {
    private static final String DEFAULT_LOG_NAME = "OPERATION";
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudLoggingServiceLogsProvider.class);
    private final ProcessLogsPersistenceService processLogsPersistenceService;

    private static final Map<String, WebClient> deploymentClients = new HashMap<>();

    public CloudLoggingServiceLogsProvider(ProcessLogsPersistenceService processLogsPersistenceService) {
        this.processLogsPersistenceService = processLogsPersistenceService;
    }

    public void logMessage(DelegateExecution execution, String logMessage, String level) {
        try {
            getLogger(execution, DEFAULT_LOG_NAME, logMessage, level);
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    public void getLogger(DelegateExecution execution, String logName, String logMessage, String level) throws SSLException {
        ExternalLoggingServiceConfiguration externalLoggingServiceConfiguration = VariableHandling.get(execution,
                                                                                                       Variables.EXTERNAL_LOGGING_SERVICE_WEB_CLIENT);

        if (externalLoggingServiceConfiguration == null) {
            return;
        }

        WebClient webClient = createWebClientWithMtls(externalLoggingServiceConfiguration.getEndpointUrl(),
                                                      externalLoggingServiceConfiguration.getServerCa(),
                                                      externalLoggingServiceConfiguration.getClientCert(),
                                                      externalLoggingServiceConfiguration.getClientKey());

        if (!VariableHandling.get(execution, Variables.IS_LOG_CACHE_CLEARED)) {
            try {
                List<OperationLogEntry> operationLogEntries = processLogsPersistenceService.listOperationLogsBySpaceAndOperationIdAndIsSendToCloudLoggingService(
                    externalLoggingServiceConfiguration.getTargetSpace(),
                    externalLoggingServiceConfiguration.getOperationId());

                for (OperationLogEntry ope : operationLogEntries) {
                    String[] splittedString = ope.getOperationLog()
                                                 .split("(?m)^#[^#\\r\\n]*#[^#\\r\\n]*#[^#\\r\\n]*#[^#\\r\\n]*#[^#\\r\\n]*#(?:\\r?\\n)?");
                    for (String s : splittedString) {
                        if (!s.isEmpty() && !s.isBlank()) {
                            s = s.substring(s.indexOf("]") + 1)
                                 .trim();
                            s = s.substring(0, s.length() - 1);
                            //                            LOGGER.warn("loggging\n" + s);
                        }
                    }
                }

                VariableHandling.set(execution, Variables.IS_LOG_CACHE_CLEARED, true);
            } catch (FileStorageException e) {
                throw new RuntimeException(e);
            }
        }

        logMessage = logMessage.substring(logMessage.indexOf("]") + 1)
                               .trim();
        logMessage = logMessage.substring(0, logMessage.length() - 1);
        //        LOGGER.warn("loggging from somewhere else\n" + logMessage);

        ExternalOperationLogEntry externalOperationLogEntry = ImmutableExternalOperationLogEntry.builder()
                                                                                                .id(UUID.randomUUID()
                                                                                                        .toString())
                                                                                                .correlationId(
                                                                                                    externalLoggingServiceConfiguration.getOperationId())
                                                                                                .message(logMessage)
                                                                                                .timestamp(LocalDateTime.now()
                                                                                                                        .toString())
                                                                                                .build();

    }

    private WebClient createWebClientWithMtls(String endpointUrl, String serverCa, String clientCert, String clientKey)
        throws SSLException {

        // Convert PEM strings to InputStreams
        InputStream serverCaStream = new ByteArrayInputStream(serverCa.getBytes(StandardCharsets.UTF_8));
        InputStream clientCertStream = new ByteArrayInputStream(clientCert.getBytes(StandardCharsets.UTF_8));
        InputStream clientKeyStream = new ByteArrayInputStream(clientKey.getBytes(StandardCharsets.UTF_8));

        // Create SSL context with client certificate and server CA
        SslContext sslContext = SslContextBuilder.forClient()
                                                 .keyManager(clientCertStream,
                                                             clientKeyStream)  // Client certificate and private key for mTLS
                                                 .trustManager(serverCaStream)                   // Server CA certificate for trust
                                                 .build();

        // Create HTTP client with custom SSL context
        HttpClient httpClient = HttpClient.create()
                                          .secure(sslSpec -> sslSpec.sslContext(sslContext));

        // Build WebClient with the custom HTTP client
        return WebClient.builder()
                        .baseUrl(endpointUrl)
                        .clientConnector(new ReactorClientHttpConnector(httpClient))
                        .build();
    }
}