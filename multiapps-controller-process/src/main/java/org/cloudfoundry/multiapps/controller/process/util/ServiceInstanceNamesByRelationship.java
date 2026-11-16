package org.cloudfoundry.multiapps.controller.process.util;

import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;

/**
 * Builds the {@code relationship name -> CF service instance name} map used by {@link ServiceKeyCredentialResolver} to turn the resource
 * name in a {@code {ds/<resource>/...}} reference into the actual service instance name to query.
 */
public final class ServiceInstanceNamesByRelationship {

    private ServiceInstanceNamesByRelationship() {
    }

    public static Map<String, String> fromDescriptor(DeploymentDescriptor descriptor) {
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

}
