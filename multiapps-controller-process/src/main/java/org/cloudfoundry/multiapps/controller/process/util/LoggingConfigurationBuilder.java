package org.cloudfoundry.multiapps.controller.process.util;

import java.text.MessageFormat;
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
import org.cloudfoundry.multiapps.controller.core.util.NameUtil;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableLoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.LogLevel;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.Resource;

public class LoggingConfigurationBuilder {

    private static final String CREDENTIAL_KEY_INGEST_MTLS_ENDPOINT = "ingest-mtls-endpoint";
    private static final String CREDENTIAL_KEY_SERVER_CA = "server-ca";
    private static final String CREDENTIAL_KEY_INGEST_MTLS_CERT = "ingest-mtls-cert";
    private static final String CREDENTIAL_KEY_INGEST_MTLS_KEY = "ingest-mtls-key";

    private final CloudControllerClientFactory clientFactory;
    private final ProcessContext context;
    private final TokenService tokenService;

    public LoggingConfigurationBuilder(CloudControllerClientFactory clientFactory, ProcessContext context,
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
        String spaceName = getTargetSpace(resource, context.getVariable(Variables.SPACE_NAME));
        String orgName = getTargetOrg(resource, context.getVariable(Variables.ORGANIZATION_NAME));
        String targetSpaceGuid = resolveTargetSpaceGuid(orgName, spaceName);
        LogLevel logLevel = getLogLevelsFromConfiguration(resource);

        return ImmutableLoggingConfiguration.copyOf(loggingConfiguration)
                                            .withId(UUID.randomUUID()
                                                        .toString())
                                            .withOperationId(correlationId)
                                            .withTargetSpace(spaceName)
                                            .withTargetSpaceGuid(targetSpaceGuid)
                                            .withTargetOrg(orgName)
                                            .withMtaId(context.getVariable(Variables.MTA_ID))
                                            .withMtaSpaceId(context.getVariable(Variables.SPACE_GUID))
                                            .withMtaSpace(context.getVariable(Variables.SPACE_NAME))
                                            .withMtaOrg(context.getVariable(Variables.ORGANIZATION_NAME))
                                            .withNamespace(context.getVariable(Variables.MTA_NAMESPACE))
                                            .withLogLevel(logLevel)
                                            .withIsFailSafe(resource.isOptional());
    }

    public LoggingConfiguration exportOperationLogsToExternalSystem(LoggingConfiguration incomingLoggingConfiguration,
                                                                    ProcessContext context) {
        return getCredentialsFromServiceKey(incomingLoggingConfiguration, context);
    }

    private LogLevel getLogLevelsFromConfiguration(Resource resource) {
        if (!resource.getParameters()
                     .containsKey(SupportedParameters.LOG_LEVEL)) {
            return LogLevel.INFO;
        }
        String logLevelFromDescriptor = MiscUtil.cast(resource.getParameters()
                                                              .get(SupportedParameters.LOG_LEVEL));
        if (LogLevel.isValid(logLevelFromDescriptor)) {
            return LogLevel.get(logLevelFromDescriptor);
        }
        if (resource.isOptional()) {
            return null;
        } else {
            throw new SLException(Messages.INVALID_LOG_LEVEL);
        }
    }

    private boolean areCloudLoggingParametersInvalid(String serviceInstanceName, String serviceKeyName) {
        return serviceInstanceName == null || serviceInstanceName.isBlank() || serviceKeyName == null || serviceKeyName.isBlank();
    }

    private String getServiceKeyName(Resource resource) {
        return MiscUtil.cast(resource.getParameters()
                                     .get(SupportedParameters.SERVICE_KEY_NAME));
    }

    private LoggingConfiguration getCredentialsFromServiceKey(LoggingConfiguration loggingConfiguration, ProcessContext context) {
        CloudServiceKey loggingServiceKey = getServiceKeyWithLoggingConfiguration(loggingConfiguration);
        if (loggingServiceKey == null) {
            return null;
        }
        Map<String, Object> credentials = loggingServiceKey.getCredentials();

        String endpoint = getCredentialFromServiceKey(CREDENTIAL_KEY_INGEST_MTLS_ENDPOINT, credentials);
        String serverCa = getCredentialFromServiceKey(CREDENTIAL_KEY_SERVER_CA, credentials);
        String ingestMtlsCert = getCredentialFromServiceKey(CREDENTIAL_KEY_INGEST_MTLS_CERT, credentials);
        String ingestMtlsKey = getCredentialFromServiceKey(CREDENTIAL_KEY_INGEST_MTLS_KEY, credentials);

        return ImmutableLoggingConfiguration.copyOf(loggingConfiguration)
                                            .withOperationId(context.getVariable(Variables.CORRELATION_ID))
                                            .withMtaSpaceId(context.getVariable(Variables.SPACE_GUID))
                                            .withServiceInstanceGuid(toGuidString(loggingServiceKey.getServiceInstance()
                                                                                                   .getGuid()))
                                            .withServerCa(serverCa)
                                            .withEndpointUrl(endpoint)
                                            .withClientCert(ingestMtlsCert)
                                            .withClientKey(ingestMtlsKey);
    }

    private CloudServiceKey getServiceKeyWithResource(Resource resource) {
        return getCloudLoggingServiceKey(NameUtil.getServiceInstanceNameOrDefault(resource), getServiceKeyName(resource),
                                         getTargetOrg(resource, context.getVariable(Variables.ORGANIZATION_NAME)),
                                         getTargetSpace(resource, context.getVariable(Variables.SPACE_NAME)),
                                         resource.isOptional());
    }

    private CloudServiceKey getServiceKeyWithLoggingConfiguration(LoggingConfiguration loggingConfiguration) {
        return getCloudLoggingServiceKey(loggingConfiguration.getServiceInstanceName(), loggingConfiguration.getServiceKeyName(),
                                         loggingConfiguration.getTargetOrg(), loggingConfiguration.getTargetSpace(),
                                         loggingConfiguration.isFailSafe());
    }

    private CloudServiceKey getCloudLoggingServiceKey(String serviceInstanceName, String serviceKeyName, String destinationOrg,
                                                      String destinationSpace, boolean isFailSafe) {
        if (areCloudLoggingParametersInvalid(serviceInstanceName, serviceKeyName)) {
            return handleMissingServiceKey(isFailSafe, null);
        }
        CloudControllerClient client = calculateExternalLoggingServiceConfiguration(destinationOrg, destinationSpace);
        try {
            CloudServiceKey loggingServiceKey = client.getServiceKey(serviceInstanceName, serviceKeyName);
            if (loggingServiceKey != null) {
                return loggingServiceKey;
            }
            return handleMissingServiceKey(isFailSafe, null);
        } catch (CloudOperationException e) {
            return handleMissingServiceKey(isFailSafe, e);
        }
    }

    private CloudServiceKey handleMissingServiceKey(boolean isFailSafe, CloudOperationException cause) {
        if (isFailSafe) {
            return null;
        }
        if (cause != null) {
            throw new SLException(cause);
        }
        throw new SLException(MessageFormat.format(Messages.NO_CLOUD_LOGGING_SERVICE_KEY_FOUND_FOR_OPERATION_0_SKIPPING_LOG_EXPORT,
                                                   context.getVariable(Variables.CORRELATION_ID)));
    }

    private LoggingConfiguration getCredentialsFromServiceKey(Resource resource) {
        CloudServiceKey loggingServiceKey = getServiceKeyWithResource(resource);
        if (loggingServiceKey == null) {
            return null;
        }
        Map<String, Object> credentials = loggingServiceKey.getCredentials();

        String endpoint = getCredentialFromServiceKey(CREDENTIAL_KEY_INGEST_MTLS_ENDPOINT, credentials);
        String serverCa = getCredentialFromServiceKey(CREDENTIAL_KEY_SERVER_CA, credentials);
        String ingestMtlsCert = getCredentialFromServiceKey(CREDENTIAL_KEY_INGEST_MTLS_CERT, credentials);
        String ingestMtlsKey = getCredentialFromServiceKey(CREDENTIAL_KEY_INGEST_MTLS_KEY, credentials);

        return ImmutableLoggingConfiguration.builder()
                                            .serverCa(serverCa)
                                            .endpointUrl(endpoint)
                                            .clientKey(ingestMtlsKey)
                                            .clientCert(ingestMtlsCert)
                                            .serviceInstanceName(NameUtil.getServiceInstanceNameOrDefault(resource))
                                            .serviceInstanceGuid(toGuidString(loggingServiceKey.getServiceInstance()
                                                                                               .getGuid()))
                                            .serviceKeyName(loggingServiceKey.getName())
                                            .build();
    }

    private String getCredentialFromServiceKey(String credentialsName, Map<String, Object> credentials) {
        String credential = (String) credentials.get(credentialsName);

        if (credential == null) {
            throw new SLException(MessageFormat.format(Messages.MISSING_REQUIRED_1_CREDENTIAL_FROM_SCL_EXPORT, credentialsName));
        }

        return credential;
    }

    private CloudControllerClient calculateExternalLoggingServiceConfiguration(String destinationOrg, String destinationSpace) {
        String currentTargetOrg = context.getVariable(Variables.ORGANIZATION_NAME);
        String currentTargetSpace = context.getVariable(Variables.SPACE_NAME);

        String targetOrg = getTargetOrg(destinationOrg, currentTargetOrg);
        String targetSpace = getTargetSpace(destinationSpace, currentTargetSpace);

        if (targetOrg.equals(currentTargetOrg) && targetSpace.equals(currentTargetSpace)) {
            return context.getControllerClient();
        }
        return clientFactory.createClient(tokenService.getToken(context.getVariable(Variables.USER_GUID)), targetOrg, targetSpace,
                                          context.getVariable(Variables.CORRELATION_ID));
    }

    private String resolveTargetSpaceGuid(String destinationOrg, String destinationSpace) {
        CloudControllerClient client = calculateExternalLoggingServiceConfiguration(destinationOrg, destinationSpace);
        return toGuidString(client.getTarget()
                                  .getGuid());
    }

    private static String toGuidString(UUID guid) {
        return guid == null ? null : guid.toString();
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
        return getDestination(resource).get(SupportedParameters.CLS_ORG_NAME) == null
            ? org
            : getDestination(resource).get(SupportedParameters.CLS_ORG_NAME)
                                      .toString();
    }

    private String getTargetSpace(Resource resource, String space) {
        Map<String, Object> destination = getDestination(resource);
        if (destination == null) {
            return space;
        }
        return destination.get(SupportedParameters.CLS_SPACE_NAME) == null
            ? space
            : destination.get(SupportedParameters.CLS_SPACE_NAME)
                         .toString();
    }

    private Map<String, Object> getDestination(Resource resource) {
        return MiscUtil.cast(resource.getParameters()
                                     .get(SupportedParameters.DESTINATION));
    }
}
