package org.cloudfoundry.multiapps.controller.process.security.resolver;

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

@Named
public class SecretTokenKeyResolverImpl implements SecretTokenKeyResolver {

    CloudControllerClientProvider cloudControllerClientProvider;

    private UUID serviceInstanceID;

    @Inject
    public SecretTokenKeyResolverImpl(CloudControllerClientProvider cloudControllerClientProvider) {
        this.cloudControllerClientProvider = cloudControllerClientProvider;
    }

    public SecretTokenKeyResolverImpl() {
    }

    private final Duration containerExpirationTime = Duration.ofMinutes(10);
    private CachedMap<String, SecretTokenKeyContainer> cachedMap = new CachedMap<>(containerExpirationTime);

    @Override
    public SecretTokenKeyContainer resolve(DelegateExecution execution) {
        SecretTokenKeyContainer secretTokenKeyContainer = null;
        if (serviceInstanceID != null) {
            secretTokenKeyContainer = cachedMap.get(serviceInstanceID.toString());
        }
        if (secretTokenKeyContainer != null) {
            return secretTokenKeyContainer;
        }

        CloudControllerClient cloudControllerClient = createCloudControllerClient(execution);
        String userProvidedServiceName = getUserProvidedServiceName(execution);

        CloudServiceInstance cloudServiceInstance = cloudControllerClient.getServiceInstance(userProvidedServiceName);
        if (cloudServiceInstance == null) {
            throw new SLException(Messages.COULD_NOT_RETRIEVE_USER_PROVIDED_SERVICE_INSTANCE_ENCRYPTION_RELATED);
        }
        this.serviceInstanceID = cloudServiceInstance.getGuid();

        Map<String, Object> serviceInstanceCredentials = getUserProvidedServiceInstanceParameters(cloudServiceInstance,
                                                                                                  cloudControllerClient);
        if (serviceInstanceCredentials == null || serviceInstanceCredentials.isEmpty()) {
            throw new SLException(Messages.COULD_NOT_RETRIEVE_CREDENTIALS_FROM_USER_PROVIDED_SERVICE_INSTANCE_ENCRYPTION_RELATED);
        }

        SecretTokenKeyContainer resultContainer = createSecretTokenKeyContainer(serviceInstanceCredentials);
        cachedMap.put(serviceInstanceID.toString(), resultContainer);
        return resultContainer;
    }

    private CloudControllerClient createCloudControllerClient(DelegateExecution execution) {
        String userGuid = StepsUtil.determineCurrentUserGuid(execution);
        String spaceGuid = VariableHandling.get(execution, Variables.SPACE_GUID);
        String correlationId = VariableHandling.get(execution, Variables.CORRELATION_ID);

        return cloudControllerClientProvider.getControllerClient(userGuid, spaceGuid, correlationId);
    }

    private String getUserProvidedServiceName(DelegateExecution execution) {
        String mtaId = VariableHandling.get(execution, Variables.MTA_ID);
        String namespace = VariableHandling.get(execution, Variables.MTA_NAMESPACE);

        if (mtaId == null) {
            throw new SLException("Missing mtaId in encryption key resolver! Cannot continue from here!");
        }

        if (namespace != null) {
            return String.format(Constants.TRIPLE_APPENDED_STRING, Constants.USER_PROVIDED_SERVICE_PREFIX_NAME_ENCRYPTION_DECRYPTION, mtaId,
                                 namespace);
        } else {
            return String.format(Constants.DOUBLE_APPENDED_STRING, Constants.USER_PROVIDED_SERVICE_PREFIX_NAME_ENCRYPTION_DECRYPTION,
                                 mtaId);
        }
    }

    private SecretTokenKeyContainer createSecretTokenKeyContainer(Map<String, Object> serviceInstanceCredentials) {
        String encryptionKey = serviceInstanceCredentials.get(Constants.ENCRYPTION_KEY)
                                                         .toString();
        if (encryptionKey.length() != 32) {
            throw new SLException(Messages.INVALID_ENCRYPTION_KEY_LENGTH);
        }

        String encryptionKeyId = serviceInstanceCredentials.get(Constants.KEY_ID)
                                                           .toString();

        return new SecretTokenKeyContainer(encryptionKey, encryptionKeyId);
    }

    private Map<String, Object> getUserProvidedServiceInstanceParameters(CloudServiceInstance cloudServiceInstance,
                                                                         CloudControllerClient cloudControllerClient) {
        UUID serviceInstanceGuid = cloudServiceInstance.getGuid();
        return cloudControllerClient.getUserProvidedServiceInstanceParameters(serviceInstanceGuid);
    }

}
