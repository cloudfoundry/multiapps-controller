package org.cloudfoundry.multiapps.controller.process.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLException;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientFactory;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableLoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

public class ExternalLoggingServiceConfigurationsCalculator {

    private StepLogger stepLogger;
    private CloudControllerClientFactory clientFactory;
    private ProcessContext context;
    private TokenService tokenService;

    public ExternalLoggingServiceConfigurationsCalculator(StepLogger stepLogger, CloudControllerClientFactory clientFactory,
                                                          ProcessContext context,
                                                          TokenService tokenService) {
        this.stepLogger = stepLogger;
        this.clientFactory = clientFactory;
        this.context = context;
        this.tokenService = tokenService;
    }

    public LoggingConfiguration exportOperationLogsToExternalSystem(Resource resource) {
        String serviceInstanceName = (String) resource.getParameters()
                                                      .get("service-name");
        CloudControllerClient client1 = calculateExternalLoggingServiceConfiguration(resource);

        String correlationId = context.getVariable(Variables.CORRELATION_ID);
        String spaceId = context.getVariable(Variables.SPACE_GUID);
        String orgId = context.getVariable(Variables.ORGANIZATION_GUID);
        CloudServiceKey loggingServiceKey = client1.getServiceKey(serviceInstanceName, resource.getName());
        if (loggingServiceKey == null) {
            throw new IllegalStateException(
                MessageFormat.format("No logging service key found for operation {0}, skipping log export", correlationId));
        }
        Map<String, Object> credentials = loggingServiceKey.getCredentials();
        String endpoint = (String) credentials.get("ingest-mtls-endpoint");
        String serverCa = (String) credentials.get("server-ca");
        String ingestMtlsCert = (String) credentials.get("ingest-mtls-cert");
        String ingestMtlsKey = (String) credentials.get("ingest-mtls-key");

        // Validate that all required credentials are present
        if (endpoint == null || serverCa == null || ingestMtlsCert == null || ingestMtlsKey == null) {
            throw new IllegalArgumentException(
                "Missing required credentials for SAP Cloud Logging export. Required: endpoint, server-ca, ingest-mtls-cert, ingest-mtls-key");
        }

        Map<String, Object> logConfig = MiscUtil.cast(resource.getParameters()
                                                              .get("log-structure"));

        String spaceName = MiscUtil.cast(logConfig.get("space-name"));
        String orgName = MiscUtil.cast(logConfig.get("org-name"));
        Boolean isFailSafe = MiscUtil.cast(logConfig.get("is-fail-safe"));

        List<String> logLevels = Arrays.stream(logConfig.get("log-levels")
                                                        .toString()
                                                        .split(", "))
                                       .toList();

        return ImmutableLoggingConfiguration.builder()
                                            .endpointUrl(endpoint)
                                            .operationId(correlationId)
                                            .serverCa(serverCa)
                                            .targetSpace(spaceName == null ? spaceId : spaceName)
                                            .targetOrg(orgName == null ? orgId : orgName)
                                            .clientCert(ingestMtlsCert)
                                            .clientKey(ingestMtlsKey)
                                            .logLevels(logLevels)
                                            .isFailSafe(isFailSafe)
                                            .build();
    }

    private CloudControllerClient calculateExternalLoggingServiceConfiguration(Resource cloudLoggingExistingServiceKey) {
        String currentTargetOrg = context.getVariable(Variables.ORGANIZATION_NAME);
        String currentTargetSpace = context.getVariable(Variables.SPACE_NAME);
        CloudControllerClient client = context.getControllerClient();
        String targetOrg = cloudLoggingExistingServiceKey.getParameters()
                                                         .get(SupportedParameters.ORGANIZATION_NAME) == null
            ? currentTargetOrg
            : (String) cloudLoggingExistingServiceKey.getParameters()
                                                     .get(SupportedParameters.ORGANIZATION_NAME);

        String targetSpace = cloudLoggingExistingServiceKey.getParameters()
                                                           .get(SupportedParameters.SPACE_NAME) == null
            ? currentTargetSpace
            : (String) cloudLoggingExistingServiceKey.getParameters()
                                                     .get(SupportedParameters.SPACE_NAME);

        if (!targetOrg.equals(currentTargetOrg) || !targetSpace.equals(currentTargetSpace)) {
            client = clientFactory.createClient(tokenService.getToken(context.getVariable(Variables.USER_GUID)), targetOrg,
                                                targetSpace, context.getVariable(Variables.CORRELATION_ID));
        }

        return client;
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
