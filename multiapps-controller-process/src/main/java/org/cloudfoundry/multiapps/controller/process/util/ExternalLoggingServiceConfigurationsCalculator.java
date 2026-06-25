package org.cloudfoundry.multiapps.controller.process.util;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
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
        if (loggingConfiguration == null) {
            return null;
        }

        String correlationId = context.getVariable(Variables.CORRELATION_ID);
        String spaceId = getTargetSpace(resource, context.getVariable(Variables.SPACE_NAME));
        String orgId = getTargetOrg(resource, context.getVariable(Variables.ORGANIZATION_NAME));
        LogLevel logLevel = getLogLevelsFromConfiguration(resource);

        return ImmutableLoggingConfiguration.copyOf(loggingConfiguration)
                                            .withId(UUID.randomUUID()
                                                        .toString())
                                            .withOperationId(correlationId)
                                            .withTargetSpace(spaceId)
                                            .withTargetOrg(orgId)
                                            .withMtaId(context.getVariable(Variables.MTA_ID))
                                            .withMtaSpaceId(context.getVariable(Variables.SPACE_GUID))
                                            .withMtaSpace(context.getVariable(Variables.SPACE_NAME))
                                            .withMtaOrg(context.getVariable(Variables.ORGANIZATION_NAME))
                                            .withNamespace(context.getVariable(Variables.MTA_NAMESPACE))
                                            .withLogLevel(logLevel)
                                            .withIsFailSafe(resource.isOptional());
    }

    public LoggingConfiguration exportOperationLogsToExternalSystem(LoggingConfiguration incommingLoggingConfiguration,
                                                                    ProcessContext context) {
        return getCredentialsFromServiceKey(incommingLoggingConfiguration, context);
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

    private CloudServiceKey getCloudLoggingServiceKey(String serviceInstanceName, String serviceKeyName, String destinationOrg,
                                                      String destinationSpace, boolean isFailSafe) {
        String correlationId = context.getVariable(Variables.CORRELATION_ID);
        if (areCloudLoggingParametersValid(serviceInstanceName, serviceKeyName)) {
            if (isFailSafe) {
                return null;
            } else {
                throw new SLException(
                    MessageFormat.format("No logging service key found for operation {0}, skipping log export", correlationId));
            }
        }
        CloudControllerClient client1 = calculateExternalLoggingServiceConfiguration(destinationOrg, destinationSpace);
        try {
            CloudServiceKey loggingServiceKey = client1.getServiceKey(serviceInstanceName, serviceKeyName);
            if (loggingServiceKey == null) {
                if (isFailSafe) {
                    return null;
                } else {
                    throw new SLException(
                        MessageFormat.format("No logging service key found for operation {0}, skipping log export", correlationId));
                }
            }
            return loggingServiceKey;
        } catch (CloudOperationException e) {
            if (isFailSafe) {
                return null;
            } else {
                throw new SLException(e);
            }
        }
    }

    private boolean areCloudLoggingParametersValid(String serviceInstanceName, String serviceKeyName) {
        return serviceInstanceName == null || serviceInstanceName.isBlank() || serviceKeyName == null || serviceKeyName.isBlank();
    }

    private String getServiceKeyName(Resource resource) {
        List<Map<String, Object>> serviceKeys = MiscUtil.cast(resource.getParameters()
                                                                      .get(SupportedParameters.SERVICE_KEYS));
        if (serviceKeys == null || serviceKeys.isEmpty()) {
            return null;
        }
        return MiscUtil.cast(serviceKeys.get(0)
                                        .get(SupportedParameters.NAME));
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

    private LoggingConfiguration getCredentialsFromServiceKey(LoggingConfiguration loggingConfiguration, ProcessContext context) {
        CloudServiceKey loggingServiceKey = getServiceKeyWithLoggingConfiguration(loggingConfiguration);
        if (loggingServiceKey == null) {
            return null;
        }
        Map<String, Object> credentials = loggingServiceKey.getCredentials();

        String endpoint = getCredentialFromServiceKey("ingest-mtls-endpoint", credentials);
        String serverCa = getCredentialFromServiceKey("server-ca", credentials);
        String ingestMtlsCert = getCredentialFromServiceKey("ingest-mtls-cert", credentials);
        String ingestMtlsKey = getCredentialFromServiceKey("ingest-mtls-key", credentials);

        return ImmutableLoggingConfiguration.copyOf(loggingConfiguration)
                                            .withOperationId(context.getVariable(Variables.CORRELATION_ID))
                                            .withMtaSpaceId(context.getVariable(Variables.SPACE_GUID))
                                            .withServerCa(serverCa)
                                            .withEndpointUrl(endpoint)
                                            .withClientCert(ingestMtlsCert)
                                            .withClientKey(ingestMtlsKey);
    }

    private CloudServiceKey getServiceKeyWithResource(Resource resource) {
        return getCloudLoggingServiceKey(getServiceInstanceName(resource), getServiceKeyName(resource),
                                         getTargetOrg(resource, context.getVariable(Variables.ORGANIZATION_NAME)),
                                         getTargetSpace(resource, context.getVariable(Variables.SPACE_NAME)),
                                         resource.isOptional());
    }

    private CloudServiceKey getServiceKeyWithLoggingConfiguration(LoggingConfiguration loggingConfiguration) {
        return getCloudLoggingServiceKey(loggingConfiguration.getServiceInstanceName(), loggingConfiguration.getServiceKeyName(),
                                         loggingConfiguration.getTargetOrg(), loggingConfiguration.getTargetSpace(),
                                         loggingConfiguration.isFailSafe());
    }

    private LoggingConfiguration getCredentialsFromServiceKey(Resource resource) {
        CloudServiceKey loggingServiceKey = getServiceKeyWithResource(resource);
        if (loggingServiceKey == null) {
            return null;
        }
        Map<String, Object> credentials = loggingServiceKey.getCredentials();

        String endpoint = getCredentialFromServiceKey("ingest-mtls-endpoint", credentials);
        String serverCa = getCredentialFromServiceKey("server-ca", credentials);
        String ingestMtlsCert = getCredentialFromServiceKey("ingest-mtls-cert", credentials);
        String ingestMtlsKey = getCredentialFromServiceKey("ingest-mtls-key", credentials);

        return ImmutableLoggingConfiguration.builder()
                                            .serverCa(serverCa)
                                            .endpointUrl(endpoint)
                                            .clientKey(ingestMtlsKey)
                                            .clientCert(ingestMtlsCert)
                                            .serviceInstanceName(getServiceInstanceName(resource))
                                            .serviceKeyName(loggingServiceKey.getName())
                                            .build();
    }

    private String getCredentialFromServiceKey(String credentialsName, Map<String, Object> credentials) {
        String credential = (String) credentials.get(credentialsName);

        if (credential == null) {
            throw new IllegalArgumentException("Missing required " + credentialsName + " credential for SAP Cloud Logging export");
        }

        return credential;
    }

    private CloudControllerClient calculateExternalLoggingServiceConfiguration(String destinationOrg, String destinationSpace) {
        String currentTargetOrg = context.getVariable(Variables.ORGANIZATION_NAME);
        String currentTargetSpace = context.getVariable(Variables.SPACE_NAME);
        CloudControllerClient client = context.getControllerClient();

        String targetOrg = getTargetOrg(destinationOrg, currentTargetOrg);
        String targetSpace = getTargetSpace(destinationSpace, currentTargetSpace);

        if (!targetOrg.equals(currentTargetOrg) || !targetSpace.equals(currentTargetSpace)) {
            client = clientFactory.createClient(tokenService.getToken(context.getVariable(Variables.USER_GUID)), targetOrg,
                                                targetSpace, context.getVariable(Variables.CORRELATION_ID));
        }

        return client;
    }

    private String getTargetOrg(String existingLoggingConfigurationOrg, String org) {
        return existingLoggingConfigurationOrg == null ? org : existingLoggingConfigurationOrg;
    }

    private String getTargetSpace(String existingLoggingConfigurationSpace, String space) {
        return existingLoggingConfigurationSpace == null ? space : existingLoggingConfigurationSpace;
    }

    private String getTargetOrg(Resource resource, String org) {
        Map<String, Object> destination = getDestination(resource);
        if (destination == null) {
            return org;
        }
        return getDestination(resource).get("org-name") == null
            ? org
            : getDestination(resource).get("org-name")
                                      .toString();
    }

    private String getTargetSpace(Resource resource, String space) {
        Map<String, Object> destination = getDestination(resource);
        if (destination == null) {
            return space;
        }
        return destination.get("space-name") == null
            ? space
            : destination.get("space-name")
                         .toString();
    }

    private Map<String, Object> getDestination(Resource resource) {
        return MiscUtil.cast(resource.getParameters()
                                     .get(SupportedParameters.DESTINATION));
    }
}
