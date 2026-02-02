package org.cloudfoundry.multiapps.controller.process.security.resolver;

import java.text.MessageFormat;
import java.time.Duration;
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
        String encryptionKey = cachedMap.get(correlationId);
        if (encryptionKey != null) {
            return encryptionKey;
        }

        CloudControllerClient cloudControllerClient = createCloudControllerClient(execution, correlationId);
        String userProvidedServiceName = getUserProvidedServiceName(execution);
        CloudServiceInstance cloudServiceInstance = cloudControllerClient.getServiceInstance(userProvidedServiceName);
        if (cloudServiceInstance == null) {
            throw new SLException(Messages.COULD_NOT_RETRIEVE_USER_PROVIDED_SERVICE_INSTANCE_ENCRYPTION_RELATED);
        }

        Map<String, Object> serviceInstanceCredentials = getUserProvidedServiceInstanceParameters(cloudServiceInstance,
                                                                                                  cloudControllerClient);
        if (serviceInstanceCredentials == null || serviceInstanceCredentials.isEmpty()) {
            throw new SLException(Messages.COULD_NOT_RETRIEVE_CREDENTIALS_FROM_USER_PROVIDED_SERVICE_INSTANCE_ENCRYPTION_RELATED);
        }

        String resultEncryptionKey = getEncryptionKeyFromCredentials(serviceInstanceCredentials);
        cachedMap.put(correlationId, resultEncryptionKey);
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
            return disposableUserProvidedServiceName;
        }

        if (mtaId == null) {
            throw new SLException(Messages.MISSING_MTA_ID_IN_ENCRYPTION_KEY_RESOLVER);
        }

        if (namespace != null) {
            String userProvidedServiceName = String.format(Constants.TRIPLE_APPENDED_STRING,
                                                           Constants.USER_PROVIDED_SERVICE_PREFIX_NAME_ENCRYPTION_DECRYPTION, mtaId,
                                                           namespace);
            LOGGER.debug(MessageFormat.format(Messages.USING_DEFAULT_USER_PROVIDED_SERVICE_0_, userProvidedServiceName));
            return userProvidedServiceName;
        } else {
            String userProvidedServiceName = String.format(Constants.DOUBLE_APPENDED_STRING,
                                                           Constants.USER_PROVIDED_SERVICE_PREFIX_NAME_ENCRYPTION_DECRYPTION,
                                                           mtaId);
            LOGGER.debug(MessageFormat.format(Messages.USING_DEFAULT_USER_PROVIDED_SERVICE_0_, userProvidedServiceName));
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
            throw new SLException(Messages.INVALID_ENCRYPTION_KEY_LENGTH);
        }

        return encryptionKey;
    }

    private Map<String, Object> getUserProvidedServiceInstanceParameters(CloudServiceInstance cloudServiceInstance,
                                                                         CloudControllerClient cloudControllerClient) {
        UUID serviceInstanceGuid = cloudServiceInstance.getGuid();
        return cloudControllerClient.getUserProvidedServiceInstanceParameters(serviceInstanceGuid);
    }

}
