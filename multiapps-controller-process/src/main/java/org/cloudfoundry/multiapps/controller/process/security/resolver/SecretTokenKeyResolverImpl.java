package org.cloudfoundry.multiapps.controller.process.security.resolver;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.model.CachedMap;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.steps.StepsUtil;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class SecretTokenKeyResolverImpl implements SecretTokenKeyResolver {

    private CloudControllerClientProvider cloudControllerClientProvider;

    private final Duration containerExpirationTime = Duration.ofMinutes(Constants.TTL_CACHE_ENTRY);

    private final CachedMap<String, String> cachedMap = new CachedMap<>(containerExpirationTime);

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretTokenKeyResolverImpl.class);

    @Inject
    public SecretTokenKeyResolverImpl(CloudControllerClientProvider cloudControllerClientProvider) {
        this.cloudControllerClientProvider = cloudControllerClientProvider;
    }

    @Override
    public String resolve(DelegateExecution execution) {
        String correlationId = VariableHandling.get(execution, Variables.CORRELATION_ID);

        final Boolean disposable = VariableHandling.get(execution, Variables.IS_DISPOSABLE_USER_PROVIDED_SERVICE_ENABLED);
        final String disposableName = VariableHandling.get(execution, Variables.DISPOSABLE_USER_PROVIDED_SERVICE_NAME);
        final String mtaId = VariableHandling.get(execution, Variables.MTA_ID);
        final String namespace = VariableHandling.get(execution, Variables.MTA_NAMESPACE);

        LOGGER.error(
            "SecretTokenKeyResolver START RESOLVE instance={} correlationId={} disposable={} disposableName={} mtaId={} namespace={}",
            instance(),
            correlationId, disposable, disposableName, mtaId, namespace);

        String encryptionKey = cachedMap.get(correlationId);
        if (encryptionKey != null) {
            LOGGER.error("SecretTokenKeyResolver CACHE_HIT instance={} correlationId={} key={}",
                         instance(), correlationId, fingerprint(encryptionKey));
            return encryptionKey;
        }
        LOGGER.error("SecretTokenKeyResolver CACHE_MISSED instance={} correlationId={}", instance(), correlationId);

        CloudControllerClient cloudControllerClient = createCloudControllerClient(execution, correlationId);
        LOGGER.error("SecretTokenKeyResolver CLIENT_CREATED instance={} correlationId={} userGuid={} spaceGuid={}",
                     instance(), correlationId, StepsUtil.determineCurrentUserGuid(execution),
                     VariableHandling.get(execution, Variables.SPACE_GUID));

        String userProvidedServiceName = getUserProvidedServiceName(execution);
        LOGGER.error("SecretTokenKeyResolver UPS_NAME_CHOSEN instance={} correlationId={} upsName='{}'",
                     instance(), correlationId, userProvidedServiceName);

        CloudServiceInstance cloudServiceInstance = cloudControllerClient.getServiceInstance(userProvidedServiceName);
        if (cloudServiceInstance == null) {
            LOGGER.error("SecretTokenKeyResolver UPS_NOT_FOUND instance={} correlationId={} upsName='{}'",
                         instance(), correlationId, userProvidedServiceName);
            throw new SLException(Messages.COULD_NOT_RETRIEVE_USER_PROVIDED_SERVICE_INSTANCE_ENCRYPTION_RELATED);
        }

        UUID upsGuid = cloudServiceInstance.getGuid();
        LOGGER.error("SecretTokenKeyResolver UPS_RESOLVED instance={} correlationId={} upsName='{}' guid={}",
                     instance(), correlationId, userProvidedServiceName, upsGuid);

        Map<String, Object> serviceInstanceCredentials = getUserProvidedServiceInstanceParameters(cloudServiceInstance,
                                                                                                  cloudControllerClient);
        if (serviceInstanceCredentials == null || serviceInstanceCredentials.isEmpty()) {
            LOGGER.error("SecretTokenKeyResolver CREDENTIALS_EMPTY instance={} correlationId={} guid={}", instance(), correlationId,
                         upsGuid);
            throw new SLException(Messages.COULD_NOT_RETRIEVE_CREDENTIALS_FROM_USER_PROVIDED_SERVICE_INSTANCE_ENCRYPTION_RELATED);
        }

        boolean hasKey = serviceInstanceCredentials.containsKey(Constants.ENCRYPTION_KEY);
        Object rawKey = serviceInstanceCredentials.get(Constants.ENCRYPTION_KEY);
        int keyLength = String.valueOf(rawKey)
                              .length();
        LOGGER.error("SecretTokenKeyResolver CREDENTIALS_PRESENT instance={} correlationId={} guid={} hasEncryptionKey={} keyLength={}",
                     instance(), correlationId, upsGuid, hasKey, keyLength);

        String resultEncryptionKey = getEncryptionKeyFromCredentials(serviceInstanceCredentials);

        LOGGER.error("SecretTokenKeyResolver KEY_RETRIEVED instance={} correlationId={} guid={} key={}",
                     instance(), correlationId, upsGuid, fingerprint(resultEncryptionKey));

        String beforePut = cachedMap.get(correlationId);
        if (beforePut != null && !beforePut.equals(resultEncryptionKey)) {
            LOGGER.error("SecretTokenKeyResolver CACHE_OVERWRITE instance={} correlationId={} oldKey={} newKey={}",
                         instance(), correlationId, fingerprint(beforePut), fingerprint(resultEncryptionKey));
        }

        cachedMap.put(correlationId, resultEncryptionKey);
        LOGGER.error("SecretTokenKeyResolver CACHE_PUT instance={} correlationId={} guid={} key={}",
                     instance(), correlationId, upsGuid, fingerprint(resultEncryptionKey));

        return resultEncryptionKey;
    }

    private CloudControllerClient createCloudControllerClient(DelegateExecution execution, String correlationId) {
        String userGuid = StepsUtil.determineCurrentUserGuid(execution);
        String spaceGuid = VariableHandling.get(execution, Variables.SPACE_GUID);

        return cloudControllerClientProvider.getControllerClient(userGuid, spaceGuid, correlationId);
    }

    private String getUserProvidedServiceName(DelegateExecution execution) {
        String mtaId = VariableHandling.get(execution, Variables.MTA_ID);
        String namespace = VariableHandling.get(execution, Variables.MTA_NAMESPACE);
        boolean isDisposableUserProvidedServiceEnabled = VariableHandling.get(execution,
                                                                              Variables.IS_DISPOSABLE_USER_PROVIDED_SERVICE_ENABLED);

        if (isDisposableUserProvidedServiceEnabled) {
            String disposableUserProvidedServiceName = VariableHandling.get(execution, Variables.DISPOSABLE_USER_PROVIDED_SERVICE_NAME);
            LOGGER.debug(MessageFormat.format(Messages.USING_DISPOSABLE_USER_PROVIDED_SERVICE_0_, disposableUserProvidedServiceName));
            LOGGER.error(Messages.USING_DISPOSABLE_USER_PROVIDED_SERVICE_0_, disposableUserProvidedServiceName);
            return disposableUserProvidedServiceName;
        }

        if (mtaId == null) {
            LOGGER.error("SecretTokenKeyResolver MISSING_MTA_ID instance={} correlationId={}", instance(),
                         VariableHandling.get(execution, Variables.CORRELATION_ID));
            throw new SLException(Messages.MISSING_MTA_ID_IN_ENCRYPTION_KEY_RESOLVER);
        }

        if (namespace != null) {
            String userProvidedServiceName = String.format(Constants.TRIPLE_APPENDED_STRING,
                                                           Constants.USER_PROVIDED_SERVICE_PREFIX_NAME_ENCRYPTION_DECRYPTION, mtaId,
                                                           namespace);
            LOGGER.debug(MessageFormat.format(Messages.USING_DEFAULT_USER_PROVIDED_SERVICE_0_, userProvidedServiceName));
            LOGGER.error(Messages.USING_DEFAULT_USER_PROVIDED_SERVICE_0_, userProvidedServiceName);
            return userProvidedServiceName;
        } else {
            String userProvidedServiceName = String.format(Constants.DOUBLE_APPENDED_STRING,
                                                           Constants.USER_PROVIDED_SERVICE_PREFIX_NAME_ENCRYPTION_DECRYPTION,
                                                           mtaId);
            LOGGER.debug(MessageFormat.format(Messages.USING_DEFAULT_USER_PROVIDED_SERVICE_0_, userProvidedServiceName));
            LOGGER.error(Messages.USING_DEFAULT_USER_PROVIDED_SERVICE_0_, userProvidedServiceName);
            return userProvidedServiceName;
        }
    }

    private String getEncryptionKeyFromCredentials(Map<String, Object> serviceInstanceCredentials) {
        String encryptionKey = "";
        if (serviceInstanceCredentials.get(Constants.ENCRYPTION_KEY) != null) {
            encryptionKey = serviceInstanceCredentials.get(Constants.ENCRYPTION_KEY)
                                                      .toString();
        }

        if (encryptionKey.length() != Constants.ENCRYPTION_KEY_LENGTH) {
            LOGGER.error("SecretTokenKeyResolver INVALID_KEY_LENGTH keyLength={} expected={}", encryptionKey.length(),
                         Constants.ENCRYPTION_KEY_LENGTH);
            throw new SLException(Messages.INVALID_ENCRYPTION_KEY_LENGTH);
        }

        return encryptionKey;
    }

    private Map<String, Object> getUserProvidedServiceInstanceParameters(CloudServiceInstance cloudServiceInstance,
                                                                         CloudControllerClient cloudControllerClient) {
        UUID serviceInstanceGuid = cloudServiceInstance.getGuid();
        LOGGER.error("SecretTokenKeyResolver RETRIEVED_CREDENTIALS instance={} guid={}", instance(), serviceInstanceGuid);
        return cloudControllerClient.getUserProvidedServiceInstanceParameters(serviceInstanceGuid);
    }

    private static String instance() {
        String index = System.getenv("CF_INSTANCE_INDEX");
        String guid = System.getenv("CF_INSTANCE_GUID");
        return (index + "/" + guid);
    }

    private static String fingerprint(String key) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                                       .digest(key.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of()
                            .formatHex(hash)
                            .substring(0, 12);
        } catch (Exception e) {
            return "Error - encryption key";
        }
    }

}