package org.cloudfoundry.multiapps.controller.core.cf.clients.v3;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.cf.clients.CloudEntityResourceMapper;
import org.cloudfoundry.multiapps.controller.core.cf.clients.CustomControllerClientErrorHandler;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataLabels;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.criteria.MtaMetadataCriteriaBuilder;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.util.MtaMetadataUtil;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaService;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaServiceKey;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceKey;

public class CustomServiceKeysClientV3 extends CustomControllerClientV3 {

    private static final String serviceKeysResourceBaseUri = "/v3/service_credential_bindings";
    private static final String serviceKeysByMetadataSelectorUri = serviceKeysResourceBaseUri + "?type=key&label_selector={value}";
    private static final String serviceKeyDetailsUri = serviceKeysResourceBaseUri + "/{value}/details";
    private static final String includeServiceInstanceResourcesParam = "&include=service_instance";

    private final CloudEntityResourceMapper resourceMapper = new CloudEntityResourceMapper();

    public CustomServiceKeysClientV3(CloudControllerClient client) {
        super(client);
    }

    public CloudServiceKey createServiceKey(CloudServiceKey key) {
        String serviceInstanceName = key.getServiceInstance()
                                        .getName();
        if (serviceInstanceName == null) {
            throw new SLException("Cannot create a service key with missing service instance name!");
        }

        CloudServiceInstance serviceInstance = client.getServiceInstance(serviceInstanceName);

        String createServiceKeyRequest = buildCreateServiceKeyRequest(key, serviceInstance);

        UUID newKeyGuid = postResource(createServiceKeyRequest, serviceKeysResourceBaseUri);

        Map<String, Object> newKeyDetails = getResource(serviceKeyDetailsUri, newKeyGuid.toString());
        Map<String, Object> newKeyCredentials = (Map<String, Object>) newKeyDetails.get("credentials");

        // just add the new guid to the key, to skip making another call
        CloudMetadata newKeyMetadata = ImmutableCloudMetadata.builder()
                                                             .guid(newKeyGuid)
                                                             .build();
        return ImmutableCloudServiceKey.builder()
                                       .from(key)
                                       .serviceInstance(serviceInstance)
                                       .metadata(newKeyMetadata)
                                       .credentials(newKeyCredentials)
                                       .build();
    }

    private String buildCreateServiceKeyRequest(CloudServiceKey key, CloudServiceInstance serviceInstance) {
        CustomCreateServiceKeyRequestV3 createRequest = new CustomCreateServiceKeyRequestV3(key, serviceInstance.getGuid());
        try {
            return new ObjectMapper().writeValueAsString(createRequest);
        } catch (JsonProcessingException e) {
            throw new SLException(e);
        }
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
        List<DeployedMtaService> detectedServices = getOnlyPopulatedServices(services);
        if (detectedServices != null) {
            uriSuffix = detectedServices.stream()
                                .map(service -> service.getGuid()
                                                       .toString())
                                .collect(Collectors.joining(","));
            uriSuffix = "&service_instance_guids=" + uriSuffix;
        }

        return getListOfResources(new ServiceKeysResponseMapper(detectedServices), serviceKeysByMetadataSelectorUri + uriSuffix,
                                  labelSelector);
    }

    private List<DeployedMtaService> getOnlyPopulatedServices(List<DeployedMtaService> services) {
        if (services == null)
            return null;

        List<DeployedMtaService> populatedServices = services.stream()
                                                             .filter(this::serviceIsNotUserProvided)
                                                             .collect(Collectors.toList());
        
        return populatedServices.isEmpty() ? null : populatedServices;
    }

    private boolean serviceIsNotUserProvided(DeployedMtaService service) {
        return service.getMetadata() != null && service.getMetadata()
                                                       .getGuid() != null;
    }

    protected class ServiceKeysResponseMapper extends ResourcesResponseMapper<DeployedMtaServiceKey> {

        List<DeployedMtaService> mtaServices;

        public ServiceKeysResponseMapper(List<DeployedMtaService> mtaServices) {
            super();
            this.mtaServices = mtaServices;
        }

        @Override
        public List<DeployedMtaServiceKey> getMappedResources() {
            return getServiceKeys();
        }

        public Map<String, CloudServiceInstance> getIncludedServiceInstancesMapping() {
            List<Object> serviceInstances = getIncludedResources().getOrDefault("service_instances", Collections.emptyList());
            return serviceInstances.stream()
                                   .map(service -> (Map<String, Object>) service)
                                   .collect(Collectors.toMap(service -> (String) service.get("guid"), resourceMapper::mapService));

        }

        public List<DeployedMtaServiceKey> getServiceKeys() {
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

    }
}
