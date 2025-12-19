package org.cloudfoundry.multiapps.controller.process.security.resolver;

import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.steps.StepsUtil;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;

@Named
public class SecretTokenKeyResolverImpl implements SecretTokenKeyResolver {

    @Inject
    CloudControllerClientProvider cloudControllerClientProvider;

    public SecretTokenKeyResolverImpl(CloudControllerClientProvider cloudControllerClientProvider) {
        this.cloudControllerClientProvider = cloudControllerClientProvider;
    }

    public SecretTokenKeyResolverImpl() {
    }

    @Override
    public SecretTokenKeyContainer resolve(DelegateExecution execution) {
        String userGuid = StepsUtil.determineCurrentUserGuid(execution);
        String spaceGuid = VariableHandling.get(execution, Variables.SPACE_GUID);
        String correlationId = VariableHandling.get(execution, Variables.CORRELATION_ID);

        CloudControllerClient cloudControllerClient = cloudControllerClientProvider.getControllerClient(userGuid, spaceGuid, correlationId);

        String mtaId = VariableHandling.get(execution, Variables.MTA_ID);
        String namespace = VariableHandling.get(execution, Variables.MTA_NAMESPACE);

        String userProvidedServiceName;

        if (namespace != null) {
            userProvidedServiceName = Constants.USER_PROVIDED_SERVICE_PREFIX_NAME_ENCRYPTION_DECRYPTION + mtaId + namespace;
        } else {
            userProvidedServiceName = Constants.USER_PROVIDED_SERVICE_PREFIX_NAME_ENCRYPTION_DECRYPTION + mtaId;
        }

        CloudServiceInstance cloudServiceInstance = cloudControllerClient.getServiceInstance(userProvidedServiceName);
        if (cloudServiceInstance == null) {
            throw new MissingUserProvidedServiceEncryptionRelatedException(
                Messages.COULD_NOT_RETRIEVE_USER_PROVIDED_SERVICE_INSTANCE_ENCRYPTION_RELATED);
        }

        UUID serviceInstanceGuid = cloudServiceInstance.getGuid();
        Map<String, Object> serviceInstanceCredentials = cloudControllerClient.getUserProvidedServiceInstanceParameters(
            serviceInstanceGuid);
        if (serviceInstanceCredentials == null || serviceInstanceCredentials.isEmpty()) {
            throw new MissingCredentialsFromUserProvidedServiceEncryptionRelated(
                Messages.COULD_NOT_RETRIEVE_CREDENTIALS_FROM_USER_PROVIDED_SERVICE_INSTANCE_ENCRYPTION_RELATED);
        }

        String encryptionKey = serviceInstanceCredentials.get(Constants.ENCRYPTION_KEY)
                                                         .toString();
        String encryptionKeyId = serviceInstanceCredentials.get(Constants.KEY_ID)
                                                           .toString();

        return new SecretTokenKeyContainer(encryptionKey, encryptionKeyId);
    }

}
