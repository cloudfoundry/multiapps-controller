package org.cloudfoundry.multiapps.controller.process.security.resolver;

import java.time.Duration;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.model.CachedMap;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.security.util.SecurityRelatedUpsUtil;
import org.cloudfoundry.multiapps.controller.process.steps.StepsUtil;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class SecretTokenKeyResolverImpl implements SecretTokenKeyResolver {

    private final CloudControllerClientProvider cloudControllerClientProvider;

    private final Duration containerExpirationTime = Duration.ofMillis(Constants.TTL_CACHE_ENTRY);

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
        String userProvidedServiceName = SecurityRelatedUpsUtil.getUserProvidedServiceName(execution, LOGGER);
        Map<String, Object> serviceInstanceCredentials = SecurityRelatedUpsUtil.getUserProvidedServiceInstanceCredentials(
            userProvidedServiceName,
            cloudControllerClient);

        String resultEncryptionKey = SecurityRelatedUpsUtil.getEncryptionKeyFromCredentials(serviceInstanceCredentials);
        cachedMap.put(correlationId, resultEncryptionKey);
        return resultEncryptionKey;
    }

    private CloudControllerClient createCloudControllerClient(DelegateExecution execution, String correlationId) {
        String userGuid = StepsUtil.determineCurrentUserGuid(execution);
        String spaceGuid = VariableHandling.get(execution, Variables.SPACE_GUID);

        return cloudControllerClientProvider.getControllerClient(userGuid, spaceGuid, correlationId);
    }

}