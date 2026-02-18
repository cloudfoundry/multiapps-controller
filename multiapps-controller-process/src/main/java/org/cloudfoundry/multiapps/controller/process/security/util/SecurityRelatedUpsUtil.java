package org.cloudfoundry.multiapps.controller.process.security.util;

import java.text.MessageFormat;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;

public class SecurityRelatedUpsUtil {

    public static Map<String, Object> getUserProvidedServiceInstanceCredentials(String userProvidedServiceName,
                                                                                CloudControllerClient cloudControllerClient) {
        CloudServiceInstance cloudServiceInstance = cloudControllerClient.getServiceInstance(userProvidedServiceName);
        if (cloudServiceInstance == null) {
            throw new SLException(Messages.COULD_NOT_RETRIEVE_USER_PROVIDED_SERVICE_INSTANCE_ENCRYPTION_RELATED);
        }

        Map<String, Object> userProvidedServiceInstanceCredentials = getUserProvidedServiceInstanceParameters(cloudServiceInstance,
                                                                                                              cloudControllerClient);
        if (userProvidedServiceInstanceCredentials == null || userProvidedServiceInstanceCredentials.isEmpty()) {
            throw new SLException(Messages.COULD_NOT_RETRIEVE_CREDENTIALS_FROM_USER_PROVIDED_SERVICE_INSTANCE_ENCRYPTION_RELATED);
        }
        return userProvidedServiceInstanceCredentials;
    }

    public static String getUserProvidedServiceName(DelegateExecution execution, Logger LOGGER) {
        String mtaId = VariableHandling.get(execution, Variables.MTA_ID);
        String namespace = VariableHandling.get(execution, Variables.MTA_NAMESPACE);
        boolean isDisposableUserProvidedServiceEnabled = VariableHandling.get(execution,
                                                                              Variables.IS_DISPOSABLE_USER_PROVIDED_SERVICE_ENABLED);

        if (isDisposableUserProvidedServiceEnabled) {
            String disposableUserProvidedServiceName = VariableHandling.get(execution, Variables.DISPOSABLE_USER_PROVIDED_SERVICE_NAME);
            LOGGER.debug(MessageFormat.format(Messages.USING_DISPOSABLE_USER_PROVIDED_SERVICE_0, disposableUserProvidedServiceName));
            return disposableUserProvidedServiceName;
        }

        return determineUserProvidedServiceName(mtaId, namespace, LOGGER);
    }

    private static String determineUserProvidedServiceName(String mtaId, String namespace, Logger LOGGER) {
        if (mtaId == null) {
            throw new SLException(Messages.MISSING_MTA_ID_IN_ENCRYPTION_KEY_RESOLVER);
        }

        String userProvidedServiceName = String.format(Constants.DOUBLE_APPENDED_STRING,
                                                       Constants.USER_PROVIDED_SERVICE_PREFIX_NAME_ENCRYPTION_DECRYPTION,
                                                       mtaId);
        if (namespace != null) {
            userProvidedServiceName = String.format(Constants.TRIPLE_APPENDED_STRING, userProvidedServiceName, Constants.STRING_SEPARATOR,
                                                    namespace);
        }

        LOGGER.debug(MessageFormat.format(Messages.USING_DEFAULT_USER_PROVIDED_SERVICE_0, userProvidedServiceName));
        return userProvidedServiceName;
    }

    public static String getEncryptionKeyFromCredentials(Map<String, Object> serviceInstanceCredentials) {
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

    private static Map<String, Object> getUserProvidedServiceInstanceParameters(CloudServiceInstance cloudServiceInstance,
                                                                                CloudControllerClient cloudControllerClient) {
        UUID serviceInstanceGuid = cloudServiceInstance.getGuid();
        return cloudControllerClient.getUserProvidedServiceInstanceParameters(serviceInstanceGuid);
    }
}
