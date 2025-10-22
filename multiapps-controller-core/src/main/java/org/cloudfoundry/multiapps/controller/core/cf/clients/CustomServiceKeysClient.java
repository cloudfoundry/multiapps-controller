package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.client.v3.serviceinstances.ServiceInstanceType;
import org.cloudfoundry.multiapps.controller.client.facade.CloudCredentials;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataLabels;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.criteria.MtaMetadataCriteriaBuilder;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.util.MtaMetadataUtil;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaService;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaServiceKey;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;

public class CustomServiceKeysClient extends CustomControllerClient {

    private static final String SERVICE_KEYS_RESOURCE_BASE_URI = "/v3/service_credential_bindings";
    private static final String SERVICE_KEYS_BY_METADATA_SELECTOR_URI = SERVICE_KEYS_RESOURCE_BASE_URI + "?type=key&label_selector={value}";
    private static final String INCLUDE_SERVICE_INSTANCE_RESOURCES_PARAM = "&include=service_instance";

    private final CloudEntityResourceMapper resourceMapper = new CloudEntityResourceMapper();

    public CustomServiceKeysClient(ApplicationConfiguration configuration, WebClientFactory webClientFactory, CloudCredentials credentials,
                                   String correlationId) {
        super(configuration, webClientFactory, credentials, correlationId);
    }

    public List<DeployedMtaServiceKey> getServiceKeysByMetadataAndGuids(String spaceGuid, String mtaId, String mtaNamespace,
                                                                        List<DeployedMtaService> services,
                                                                        List<String> existingServiceGuids) {
        String labelSelector = MtaMetadataCriteriaBuilder.builder()
                                                         .label(MtaMetadataLabels.SPACE_GUID)
                                                         .hasValue(spaceGuid)
                                                         .and()
                                                         .label(MtaMetadataLabels.MTA_NAMESPACE)
                                                         .hasValueOrIsntPresent(MtaMetadataUtil.getHashedLabel(mtaNamespace))
                                                         .and()
                                                         .label(MtaMetadataLabels.MTA_ID)
                                                         .hasValue(MtaMetadataUtil.getHashedLabel(mtaId))
                                                         .build()
                                                         .get();

        List<String> managedGuids = extractManagedServiceGuids(services);

        List<String> allServiceGuids = Stream.concat(managedGuids.stream(), existingServiceGuids.stream())
                                             .filter(Objects::nonNull)
                                             .distinct()
                                             .toList();

        if (allServiceGuids.isEmpty()) {
            return List.of();
        }

        return getServiceKeysByMetadataInternal(labelSelector, allServiceGuids);
    }

    private List<String> extractManagedServiceGuids(List<DeployedMtaService> services) {
        if (services != null) {
            List<DeployedMtaService> managed = getManagedServices(services);
            if (!managed.isEmpty()) {
                return managed.stream()
                              .map(s -> s.getGuid() != null ? s.getGuid()
                                                               .toString() : null)
                              .filter(Objects::nonNull)
                              .distinct()
                              .toList();
            }
        }
        return List.of();
    }

    private List<DeployedMtaServiceKey> getServiceKeysByMetadataInternal(String labelSelector, List<String> guids) {

        String uriSuffix = INCLUDE_SERVICE_INSTANCE_RESOURCES_PARAM
            + "&service_instance_guids=" + String.join(",", guids);

        return getListOfResources(new ServiceKeysResponseMapper(),
                                  SERVICE_KEYS_BY_METADATA_SELECTOR_URI + uriSuffix,
                                  labelSelector);
    }

    private List<DeployedMtaService> getManagedServices(List<DeployedMtaService> services) {
        if (services != null) {
            return services.stream()
                           .filter(this::serviceIsNotUserProvided)
                           .collect(Collectors.toList());
        }
        return List.of();
    }

    private boolean serviceIsNotUserProvided(DeployedMtaService service) {
        return service.getMetadata() != null && service.getMetadata()
                                                       .getGuid() != null && service.getType() == ServiceInstanceType.MANAGED;
    }

    protected class ServiceKeysResponseMapper extends ResourcesResponseMapper<DeployedMtaServiceKey> {
        public ServiceKeysResponseMapper() {
        }

        @Override
        public List<DeployedMtaServiceKey> getMappedResources() {
            Map<String, CloudServiceInstance> serviceMapping = getIncludedServiceInstancesMapping();
            return getQueriedResources().stream()
                                        .map(resource -> resourceMapper.mapServiceKeyResource(resource, serviceMapping))
                                        .collect(Collectors.toList());
        }

        public Map<String, CloudServiceInstance> getIncludedServiceInstancesMapping() {
            List<Object> serviceInstances = getIncludedResources().getOrDefault("service_instances", Collections.emptyList());

            return serviceInstances.stream()
                                   .distinct()
                                   .map(service -> (Map<String, Object>) service)
                                   .collect(Collectors.toMap(service -> (String) service.get("guid"), resourceMapper::mapService));

        }
    }
}
