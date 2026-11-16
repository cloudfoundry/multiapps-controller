package org.cloudfoundry.multiapps.controller.process.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerException;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.CloudServiceBrokerException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.springframework.http.HttpStatus;

public final class ServiceUtil {

    private ServiceUtil() {

    }

    /**
     * Builds the {@code relationship name -> CF service instance name} map used to turn the resource name in a {@code {ds/<resource>/...}}
     * reference into the actual service instance name to query.
     */
    public static Map<String, String> buildServiceInstanceNamesByRelationship(DeploymentDescriptor descriptor) {
        Map<String, String> serviceInstanceNames = new HashMap<>();
        if (descriptor == null) {
            return serviceInstanceNames;
        }
        for (Resource resource : descriptor.getResources()) {
            Object serviceName = resource.getParameters()
                                         .get(SupportedParameters.SERVICE_NAME);
            serviceInstanceNames.put(resource.getName(),
                                     serviceName instanceof String ? (String) serviceName : resource.getName());
        }
        return serviceInstanceNames;
    }

    public static List<CloudServiceKey> getExistingServiceKeys(CloudControllerClient client, CloudServiceInstanceExtended service,
                                                               StepLogger stepLogger) {
        try {
            return client.getServiceKeysWithCredentials(service.getName());
        } catch (CloudOperationException e) {
            if (!service.isOptional()) {
                if (e.getStatusCode() == HttpStatus.BAD_GATEWAY) {
                    throw new CloudServiceBrokerException(e);
                }
                throw new CloudControllerException(e);
            }
            stepLogger.warn(e, Messages.COULD_NOT_GET_SERVICE_KEYS_FOR_OPTIONAL_SERVICE, service.getName());
            return null;
        }
    }

}
