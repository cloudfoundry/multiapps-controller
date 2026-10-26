package org.cloudfoundry.multiapps.controller.process.util;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.cloudfoundry.multiapps.mta.util.DynamicParameterUtil;

/**
 * Resolves {@code {ds/<resource>/service-keys.<key>.credentials.<field>}} references found in a service key's credentials by reading the
 * referenced (already created) service key's credentials from the Cloud Controller.
 */
public class ServiceKeyCredentialResolver {

    private final CloudControllerClient client;
    private final Map<String, String> serviceInstanceNamesByRelationship;

    public ServiceKeyCredentialResolver(CloudControllerClient client, Map<String, String> serviceInstanceNamesByRelationship) {
        this.client = client;
        this.serviceInstanceNamesByRelationship = serviceInstanceNamesByRelationship;
    }

    public static Map<String, String> buildServiceInstanceNamesFromDescriptor(DeploymentDescriptor descriptor) {
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

    public static Map<String, String> buildServiceInstanceNamesFromServicesToBind(List<CloudServiceInstanceExtended> servicesToBind) {
        Map<String, String> serviceInstanceNames = new HashMap<>();
        if (servicesToBind == null) {
            return serviceInstanceNames;
        }
        for (CloudServiceInstanceExtended service : servicesToBind) {
            serviceInstanceNames.put(service.getResourceName(), service.getName());
        }
        return serviceInstanceNames;
    }

    public CloudServiceKey resolve(CloudServiceKey serviceKey) {
        Map<String, Object> credentials = serviceKey.getCredentials();
        if (credentials == null || credentials.isEmpty()) {
            return serviceKey;
        }
        Map<String, Object> resolvedCredentials = resolveReferencesInMap(credentials);
        if (resolvedCredentials == credentials) {
            return serviceKey;
        }
        return ImmutableCloudServiceKey.copyOf(serviceKey)
                                       .withCredentials(resolvedCredentials);
    }

    /**
     * Resolves {@code {ds/...}} service-key credential references in an arbitrary map (e.g. a module's service-binding config), returning a
     * new map with the resolved values. Returns the same map instance when there is nothing to resolve.
     */
    public Map<String, Object> resolveReferencesInMap(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return values;
        }
        Map<String, Object> resolved = null;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (!isServiceKeyCredentialReference(entry.getValue())) {
                continue;
            }
            if (resolved == null) {
                resolved = new LinkedHashMap<>(values);
            }
            resolved.put(entry.getKey(), resolveReference((String) entry.getValue()));
        }
        return resolved != null ? resolved : values;
    }

    private boolean isServiceKeyCredentialReference(Object value) {
        if (!(value instanceof String stringValue) || !stringValue.matches(DynamicParameterUtil.REGEX_PATTERN_FOR_DYNAMIC_PARAMETERS)) {
            return false;
        }
        return DynamicParameterUtil.isServiceKeyCredentialReference(DynamicParameterUtil.getParameterName(stringValue));
    }

    private Object resolveReference(String reference) {
        String relationshipName = DynamicParameterUtil.getRelationshipName(reference);
        String parameterName = DynamicParameterUtil.getParameterName(reference);
        String serviceKeyName = DynamicParameterUtil.getServiceKeyName(parameterName);
        String credentialFieldPath = DynamicParameterUtil.getCredentialFieldPath(parameterName);

        String serviceInstanceName = serviceInstanceNamesByRelationship.getOrDefault(relationshipName, relationshipName);
        CloudServiceKey referencedKey = client.getServiceKey(serviceInstanceName, serviceKeyName);
        return extractCredentialField(reference, referencedKey, serviceInstanceName, serviceKeyName, credentialFieldPath);
    }

    private Object extractCredentialField(String reference, CloudServiceKey referencedKey, String serviceInstanceName,
                                          String serviceKeyName, String credentialFieldPath) {
        Object current = referencedKey == null ? null : referencedKey.getCredentials();
        for (String field : credentialFieldPath.split("\\.")) {
            if (!(current instanceof Map<?, ?> currentMap) || !currentMap.containsKey(field)) {
                throw new ContentException(MessageFormat.format(Messages.UNABLE_TO_RESOLVE_SERVICE_KEY_CREDENTIAL_REFERENCE, reference,
                                                                credentialFieldPath, serviceKeyName, serviceInstanceName));
            }
            current = currentMap.get(field);
        }
        return current;
    }

}