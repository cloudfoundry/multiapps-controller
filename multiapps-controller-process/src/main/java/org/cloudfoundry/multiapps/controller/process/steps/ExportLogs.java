package org.cloudfoundry.multiapps.controller.process.steps;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLException;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientFactory;
import org.cloudfoundry.multiapps.controller.core.model.ExternalLoggingServiceConfiguration;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLogsPersistenceService;
import org.cloudfoundry.multiapps.controller.process.util.OperationLogsExporter;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Named("exportLogs")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ExportLogs extends SyncFlowableStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExportLogs.class);

    private ProcessLogsPersistenceService processLogsPersistenceService;
    private CloudControllerClientFactory clientFactory;
    private TokenService tokenService;

    @Inject
    public ExportLogs(ProcessLogsPersistenceService processLogsPersistenceService, CloudControllerClientFactory clientFactory,
                      TokenService tokenService) {
        this.processLogsPersistenceService = processLogsPersistenceService;
        this.clientFactory = clientFactory;
        this.tokenService = tokenService;
    }

    @Override
    protected StepPhase executeStep(ProcessContext context) throws Exception {
        getStepLogger().debug("Prepare to export operation logs to external logging service.");
        CloudControllerClient client = context.getControllerClient();
        List<ExternalLoggingServiceConfiguration> externalLoggingServiceConfigurations = context.getVariable(
            Variables.EXTERNAL_LOGGING_SERVICE_CONFIGURATIONS);
        String currentTargetOrg = context.getVariable(Variables.ORGANIZATION_NAME);
        String currentTargetSpace = context.getVariable(Variables.SPACE_NAME);
        for (var externalLoggingServiceConfiguration : externalLoggingServiceConfigurations) {
            String serviceInstanceName = externalLoggingServiceConfiguration.getServiceInstanceName();
            String serviceKeyName = externalLoggingServiceConfiguration.getServiceKeyName();
            String targetOrg = externalLoggingServiceConfiguration.getTargetOrg() == null
                ? currentTargetOrg
                : externalLoggingServiceConfiguration.getTargetOrg();
            String targetSpace = externalLoggingServiceConfiguration.getTargetSpace() == null
                ? currentTargetSpace
                : externalLoggingServiceConfiguration.getTargetSpace();

            if (!targetOrg.equals(currentTargetOrg) && !targetSpace.equals(currentTargetSpace)) {
                client = clientFactory.createClient(tokenService.getToken(null, context.getVariable(Variables.USER_GUID)), targetOrg,
                                                    targetSpace, context.getVariable(Variables.CORRELATION_ID));
            }
            exportOperationLogsToExternalSystem(context, client, serviceInstanceName, serviceKeyName);
        }
        return StepPhase.DONE;
    }

    private void exportOperationLogsToExternalSystem(ProcessContext context, CloudControllerClient client, String serviceInstanceName,
                                                     String serviceKeyName) {
        String correlationId = context.getVariable(Variables.CORRELATION_ID);
        String spaceId = context.getVariable(Variables.SPACE_GUID);
        CloudServiceKey loggingServiceKey = client.getServiceKey(serviceInstanceName, serviceKeyName);
        if (loggingServiceKey == null) {
            getStepLogger().warn("No logging service key found for operation {0}, skipping log export", correlationId);
            return;
        }
        LOGGER.info("Exporting operation logs to external system using service key: {}", loggingServiceKey.getName());
        Map<String, Object> credentials = loggingServiceKey.getCredentials();
        String endpoint = (String) credentials.get("ingest-mtls-endpoint");
        String serverCa = (String) credentials.get("server-ca");
        String ingestMtlsCert = (String) credentials.get("ingest-mtls-cert");
        String ingestMtlsKey = (String) credentials.get("ingest-mtls-key");

        // Validate that all required credentials are present
        if (endpoint == null && serverCa == null && ingestMtlsCert == null && ingestMtlsKey == null) {
            getStepLogger().warn(
                "Missing required credentials for SAP Cloud Logging export. Required: endpoint, server-ca, ingest-mtls-cert, ingest-mtls-key");
            return;
        }
        try {
            OperationLogsExporter exporter = new OperationLogsExporter(processLogsPersistenceService,
                                                                       createWebClientWithMtls(endpoint, serverCa, ingestMtlsCert,
                                                                                               ingestMtlsKey));
            exporter.exportLogs(spaceId, correlationId, endpoint, serverCa, ingestMtlsCert, ingestMtlsKey);
            getStepLogger().info("Export of operation logs to external service instance \"{0}\" was successful", serviceInstanceName);
        } catch (Exception e) {
            getStepLogger().warn(e, "Export of operation logs to external service instance \"{0}\" failed: {1}", serviceInstanceName,
                                 e.getMessage());
        }

    }

    private WebClient createWebClientWithMtls(String endpointUrl, String serverCa, String clientCert, String clientKey)
        throws SSLException {
        LOGGER.debug("Creating WebClient with mTLS configuration for endpoint: {}", endpointUrl);

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

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return "Failure during export logs to external logging service.";
    }
}
