package com.sap.cloud.lm.sl.cf.core.cf.v1_0;

import static com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil.isService;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.domain.ServiceKey;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Resource;
import com.sap.cloud.lm.sl.mta.util.ValidatorUtil;

public class ServiceKeysCloudModelBuilder {

    protected DeploymentDescriptor deploymentDescriptor;
    protected PropertiesAccessor propertiesAccessor;

    public ServiceKeysCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, PropertiesAccessor propertiesAccessor) {
        this.deploymentDescriptor = deploymentDescriptor;
        this.propertiesAccessor = propertiesAccessor;
    }

    public Map<String, List<ServiceKey>> build() throws ContentException {
        Map<String, List<ServiceKey>> serviceKeys = new HashMap<>();
        for (Resource resource : deploymentDescriptor.getResources1_0()) {
            if (isService(resource, propertiesAccessor)) {
                serviceKeys.put(resource.getName(), getServiceKeysForService(resource));
            }
        }
        return serviceKeys;
    }

    protected List<ServiceKey> getServiceKeysForService(Resource resource) throws ContentException {
        List<Map<String, Object>> serviceKeysMaps = getServiceKeysMaps(resource);
        return serviceKeysMaps.stream()
            .map(keysMap -> getServiceKey(resource, keysMap))
            .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    protected ServiceKey getServiceKey(Resource resource, Map<String, Object> serviceKeyMap) {
        String serviceKeyName = (String) serviceKeyMap.get(SupportedParameters.NAME);
        Map<String, Object> parameters = (Map<String, Object>) serviceKeyMap.get(SupportedParameters.SERVICE_KEY_CONFIG);
        if (parameters == null) {
            parameters = Collections.emptyMap();
        }
        return new ServiceKey(serviceKeyName, parameters, Collections.emptyMap(), new CloudServiceExtended(null, resource.getName()));
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> getServiceKeysMaps(Resource resource) throws ContentException {
        Object serviceKeys = propertiesAccessor.getParameters(resource)
            .get(SupportedParameters.SERVICE_KEYS);
        if (serviceKeys == null) {
            return Collections.emptyList();
        }
        if (!(serviceKeys instanceof List)) {
            throw new ContentException(getInvalidServiceKeysErrorMessage(resource.getName(), serviceKeys));
        }
        return (List<Map<String, Object>>) serviceKeys;
    }

    protected String getInvalidServiceKeysErrorMessage(String serviceName, Object serviceConfig) {
        return MessageFormat.format(com.sap.cloud.lm.sl.mta.message.Messages.INVALID_TYPE_FOR_KEY,
            ValidatorUtil.getPrefixedName(serviceName, SupportedParameters.SERVICE_KEYS), Map.class.getSimpleName(),
            serviceConfig.getClass()
                .getSimpleName());
    }
}
