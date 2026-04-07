package org.cloudfoundry.multiapps.controller.process.util;

import java.text.MessageFormat;
import java.util.Map;

import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientFactory;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableLoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.LogLevel;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.Resource;

public class ExternalLoggingServiceConfigurationsCalculator {

    private final CloudControllerClientFactory clientFactory;
    private final ProcessContext context;
    private final TokenService tokenService;

    public ExternalLoggingServiceConfigurationsCalculator(CloudControllerClientFactory clientFactory, ProcessContext context,
                                                          TokenService tokenService) {
        this.clientFactory = clientFactory;
        this.context = context;
        this.tokenService = tokenService;
    }

    public LoggingConfiguration exportOperationLogsToExternalSystem(Resource resource) {
        LoggingConfiguration loggingConfiguration = getCredentialsFromServiceKey(resource);

        String correlationId = context.getVariable(Variables.CORRELATION_ID);
        String spaceId = getTargetSpace(resource, context.getVariable(Variables.SPACE_GUID));
        String orgId = getTargetOrg(resource, context.getVariable(Variables.ORGANIZATION_GUID));
        LogLevel logLevel = getLogLevelsFromConfiguration(resource);

        return ImmutableLoggingConfiguration.copyOf(loggingConfiguration)
                                            .withOperationId(correlationId)
                                            .withTargetSpace(spaceId)
                                            .withTargetOrg(orgId)
                                            .withLogLevel(logLevel)
                                            .withIsFailSafe(resource.isOptional());
    }

    private LogLevel getLogLevelsFromConfiguration(Resource resource) {
        LogLevel logLevel = LogLevel.INFO;
        if (resource.getParameters()
                    .containsKey(SupportedParameters.LOG_LEVEL)) {
            String logLevelFromDescriptor = MiscUtil.cast(resource.getParameters()
                                                                  .get(SupportedParameters.LOG_LEVEL));
            logLevel = LogLevel.get(logLevelFromDescriptor);
        }
        return logLevel;
    }

    private CloudServiceKey getCloudLoggingServiceKey(Resource resource) {
        String correlationId = context.getVariable(Variables.CORRELATION_ID);
        String serviceInstanceName = getServiceInstanceName(resource);
        String serviceKeyName = getServiceKeyName(resource);
        CloudControllerClient client1 = calculateExternalLoggingServiceConfiguration(resource);
        CloudServiceKey loggingServiceKey = client1.getServiceKey(serviceInstanceName, serviceKeyName);
        if (loggingServiceKey == null) {
            throw new IllegalStateException(
                MessageFormat.format("No logging service key found for operation {0}, skipping log export", correlationId));
        }

        return loggingServiceKey;
    }

    private String getServiceKeyName(Resource resource) {
        Map<String, Object> serviceKeys = MiscUtil.cast(resource.getParameters()
                                                                .get(SupportedParameters.SERVICE_KEYS));
        return MiscUtil.cast(serviceKeys.get(SupportedParameters.NAME));
    }

    private String getServiceInstanceName(Resource resource) {
        if (resource.getParameters()
                    .containsKey(SupportedParameters.SERVICE_NAME)) {
            return MiscUtil.cast(resource.getParameters()
                                         .get(SupportedParameters.SERVICE_NAME));
        } else {
            return resource.getName();
        }
    }

    private LoggingConfiguration getCredentialsFromServiceKey(Resource resource) {
        CloudServiceKey loggingServiceKey = getCloudLoggingServiceKey(resource);
        Map<String, Object> credentials = loggingServiceKey.getCredentials();

        String endpoint = getCredentialFromServiceKey("ingest-mtls-endpoint", credentials);
        String serverCa = getCredentialFromServiceKey("server-ca", credentials);
        String ingestMtlsCert = getCredentialFromServiceKey("ingest-mtls-cert", credentials);
        String ingestMtlsKey = getCredentialFromServiceKey("ingest-mtls-key", credentials);

        return ImmutableLoggingConfiguration.builder()
                                            .endpointUrl(endpoint)
                                            .serverCa(serverCa)
                                            .clientCert(ingestMtlsCert)
                                            .clientKey(ingestMtlsKey)
                                            .build();
    }

    private String getCredentialFromServiceKey(String credentialsName, Map<String, Object> credentials) {
        String credential = (String) credentials.get(credentialsName);

        if (credential == null) {
            throw new IllegalArgumentException("Missing required " + credentialsName + " credential for SAP Cloud Logging export");
        }

        return credential;
    }

    private CloudControllerClient calculateExternalLoggingServiceConfiguration(Resource cloudLoggingExistingServiceKey) {
        String currentTargetOrg = context.getVariable(Variables.ORGANIZATION_NAME);
        String currentTargetSpace = context.getVariable(Variables.SPACE_NAME);
        CloudControllerClient client = context.getControllerClient();

        String targetOrg = getTargetOrg(cloudLoggingExistingServiceKey, currentTargetOrg);
        String targetSpace = getTargetSpace(cloudLoggingExistingServiceKey, currentTargetSpace);

        if (!targetOrg.equals(currentTargetOrg) || !targetSpace.equals(currentTargetSpace)) {
            client = clientFactory.createClient(tokenService.getToken(context.getVariable(Variables.USER_GUID)), targetOrg,
                                                targetSpace, context.getVariable(Variables.CORRELATION_ID));
        }

        return client;
    }

    private String getTargetOrg(Resource resource, String org) {
        return getDestination(resource).get(SupportedParameters.ORGANIZATION_NAME) == null
            ? org
            : (String) resource.getParameters()
                               .get(SupportedParameters.ORGANIZATION_NAME);
    }

    private String getTargetSpace(Resource resource, String space) {
        return getDestination(resource).get(SupportedParameters.SPACE_NAME) == null
            ? space
            : (String) resource.getParameters()
                               .get(SupportedParameters.SPACE_NAME);
    }

    private Map<String, Object> getDestination(Resource resource) {
        return MiscUtil.cast(resource.getParameters()
                                     .get(SupportedParameters.DESTINATION));
    }
}
