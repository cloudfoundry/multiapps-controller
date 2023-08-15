package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.cloudfoundry.client.v3.serviceinstances.ServiceInstanceType;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataLabels;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.criteria.MtaMetadataCriteriaBuilder;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.util.MtaMetadataUtil;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaService;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaServiceKey;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;

import com.sap.cloudfoundry.client.facade.CloudCredentials;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;

public class CustomServiceKeysClient extends CustomControllerClient {

    private static final String serviceKeysResourceBaseUri = "/v3/service_credential_bindings";
    private static final String serviceKeysByMetadataSelectorUri = serviceKeysResourceBaseUri + "?type=key&label_selector={value}";
    private static final String includeServiceInstanceResourcesParam = "&include=service_instance";

    private final CloudEntityResourceMapper resourceMapper = new CloudEntityResourceMapper();

    public CustomServiceKeysClient(ApplicationConfiguration configuration, WebClientFactory webClientFactory, CloudCredentials credentials,
                                   String correlationId) {
        super(configuration, webClientFactory, credentials, correlationId);
    }

    public List<DeployedMtaServiceKey> getServiceKeysByMetadataAndGuids(String spaceGuid, String mtaId, String mtaNamespace,
                                                                        List<DeployedMtaService> services) {
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

        return new CustomControllerClientErrorHandler().handleErrorsOrReturnResult(() -> getServiceKeysByMetadataInternal(labelSelector,
                                                                                                                          services));
    }

    private List<DeployedMtaServiceKey> getServiceKeysByMetadataInternal(String labelSelector, List<DeployedMtaService> services) {
        String uriSuffix = includeServiceInstanceResourcesParam;
        List<DeployedMtaService> managedServices = getManagedServices(services);
        if (managedServices != null) {
            uriSuffix += "&service_instance_guids=" + managedServices.stream()
                                                                     .map(service -> service.getGuid()
                                                                                            .toString())
                                                                     .collect(Collectors.joining(","));
        }
        return getListOfResources(new ServiceKeysResponseMapper(managedServices), serviceKeysByMetadataSelectorUri + uriSuffix,
                                  labelSelector);
    }

    private List<DeployedMtaService> getManagedServices(List<DeployedMtaService> services) {
        if (services == null) {
            return null;
        }
        List<DeployedMtaService> managedServices = services.stream()
                                                           .filter(this::serviceIsNotUserProvided)
                                                           .collect(Collectors.toList());
        return managedServices.isEmpty() ? null : managedServices;
    }

    private boolean serviceIsNotUserProvided(DeployedMtaService service) {
        return service.getMetadata() != null && service.getMetadata()
                                                       .getGuid() != null
            && service.getType() == ServiceInstanceType.MANAGED;
    }

    protected class ServiceKeysResponseMapper extends ResourcesResponseMapper<DeployedMtaServiceKey> {

        List<DeployedMtaService> mtaServices;

        public ServiceKeysResponseMapper(List<DeployedMtaService> mtaServices) {
            this.mtaServices = mtaServices;
        }

        @Override
        public List<DeployedMtaServiceKey> getMappedResources() {
            Map<String, CloudServiceInstance> serviceMapping;
            if (mtaServices != null) {
                serviceMapping = mtaServices.stream()
                                            .collect(Collectors.toMap(service -> service.getGuid()
                                                                                        .toString(),
                                                                      Function.identity()));
            } else {
                serviceMapping = getIncludedServiceInstancesMapping();
            }
            return getQueriedResources().stream()
                                        .map(resource -> resourceMapper.mapServiceKeyResource(resource, serviceMapping))
                                        .collect(Collectors.toList());
        }

        public Map<String, CloudServiceInstance> getIncludedServiceInstancesMapping() {
            List<Object> serviceInstances = getIncludedResources().getOrDefault("service_instances", Collections.emptyList());
            return serviceInstances.stream()
                                   .map(service -> (Map<String, Object>) service)
                                   .collect(Collectors.toMap(service -> (String) service.get("guid"), resourceMapper::mapService));

        }
    }
}
