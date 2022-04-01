package org.cloudfoundry.multiapps.controller.core.cf.v2;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.util.CloudModelBuilderUtil;
import org.cloudfoundry.multiapps.controller.core.util.NameUtil;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;

import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceKey;

public class ServiceKeysCloudModelBuilder {

    protected final DeploymentDescriptor descriptor;
    private final String namespace;
    private final String spaceGuid;

    public ServiceKeysCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, String namespace, String spaceGuid) {
        this.descriptor = deploymentDescriptor;
        this.namespace = namespace;
        this.spaceGuid = spaceGuid;
    }

    public Map<String, List<CloudServiceKey>> build() {
        return descriptor.getResources()
                                   .stream()
                                   .filter(CloudModelBuilderUtil::isService)
                                   .collect(Collectors.toMap(Resource::getName, this::getServiceKeysForService));
    }

    protected List<CloudServiceKey> getServiceKeysForService(Resource resource) {
        List<Map<String, Object>> serviceKeysMaps = getServiceKeysMaps(resource);
        return serviceKeysMaps.stream()
                              .map(keysMap -> getServiceKey(resource, keysMap))
                              .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    protected CloudServiceKey getServiceKey(Resource resource, Map<String, Object> serviceKeyMap) {
        String serviceKeyName = (String) serviceKeyMap.get(SupportedParameters.NAME);
        Map<String, Object> parameters = (Map<String, Object>) serviceKeyMap.get(SupportedParameters.SERVICE_KEY_CONFIG);
        if (parameters == null) {
            parameters = Collections.emptyMap();
        }
        String serviceName = NameUtil.getServiceName(resource);
        Metadata serviceKeyMetadata = ServiceKeyMetadataBuilder.build(descriptor, namespace, spaceGuid);

        return ImmutableCloudServiceKey.builder()
                                       .name(serviceKeyName)
                                       .credentials(parameters)
                                       .v3Metadata(serviceKeyMetadata)
                                       .serviceInstance(ImmutableCloudServiceInstanceExtended.builder()
                                                                                             .name(serviceName)
                                                                                             .build())
                                       .build();
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> getServiceKeysMaps(Resource resource) {
        Object serviceKeys = resource.getParameters()
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
        return MessageFormat.format(org.cloudfoundry.multiapps.mta.Messages.INVALID_TYPE_FOR_KEY,
                                    org.cloudfoundry.multiapps.mta.util.NameUtil.getPrefixedName(serviceName, SupportedParameters.SERVICE_KEYS), Map.class.getSimpleName(),
                                    serviceConfig.getClass()
                                                 .getSimpleName());
    }
}
