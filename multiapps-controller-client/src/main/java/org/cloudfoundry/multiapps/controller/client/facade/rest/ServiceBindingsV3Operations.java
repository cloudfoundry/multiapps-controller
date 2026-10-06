package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.cloudfoundry.multiapps.controller.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceBinding;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Metadata;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Fields;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3GuidReference;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceBinding;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceBindingMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ServiceBindingsV3Operations {

    private static final ParameterizedTypeReference<V3ListResponse<V3ServiceBinding>> BINDING_PAGE = new ParameterizedTypeReference<>() {
    };

    private static final ParameterizedTypeReference<V3ListResponse<V3GuidReference>> GUID_REFERENCE_PAGE = new ParameterizedTypeReference<>() {
    };

    private final CloudControllerV3Client client;

    private final CloudSpace targetSpace;

    public ServiceBindingsV3Operations(CloudControllerV3Client client, CloudSpace targetSpace) {
        this.client = client;
        this.targetSpace = targetSpace;
    }

    public Optional<String> bindServiceInstance(String bindingName, String applicationName, String serviceInstanceName) {
        return bindServiceInstance(bindingName, applicationName, serviceInstanceName, null);
    }

    public Optional<String> bindServiceInstance(String bindingName, String applicationName, String serviceInstanceName,
                                                Map<String, Object> parameters) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        UUID serviceInstanceGuid = getRequiredServiceInstanceGuid(serviceInstanceName);
        Map<String, Object> serviceInstanceBody = OperationsUtil.buildServiceInstanceBody(bindingName, applicationGuid, serviceInstanceGuid,
                                                                                          parameters);

        ResponseEntity<Void> response = client.post(CloudControllerV3Endpoints.SERVICE_CREDENTIAL_BINDINGS, serviceInstanceBody);

        return OperationsUtil.extractJobGuid(response);
    }

    public List<String> unbindServiceInstance(String applicationName, String serviceInstanceName) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        UUID serviceInstanceGuid = getRequiredServiceInstanceGuid(serviceInstanceName);

        return unbindServiceInstance(applicationGuid, serviceInstanceGuid);
    }

    public CloudServiceBinding getServiceBinding(UUID serviceBindingGuid) {
        String uri = CloudControllerV3Endpoints.SERVICE_CREDENTIAL_BINDINGS + CloudControllerV3Endpoints.QUERY_GUIDS + serviceBindingGuid
            + CloudControllerV3Endpoints.AMPERSAND_PER_PAGE + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE;

        return client.list(uri, BINDING_PAGE)
                     .stream()
                     .findFirst()
                     .map(V3ServiceBindingMapper::toCloudServiceBinding)
                     .orElse(null);
    }

    public List<CloudServiceBinding> getServiceAppBindings(UUID serviceInstanceGuid) {
        String uri = CloudControllerV3Endpoints.SERVICE_CREDENTIAL_BINDINGS + CloudControllerV3Endpoints.QUERY_SERVICE_INSTANCE_GUIDS
            + serviceInstanceGuid + CloudControllerV3Endpoints.AMPERSAND_TYPE + "app" + CloudControllerV3Endpoints.AMPERSAND_PER_PAGE
            + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE;

        return listBindings(uri);
    }

    public List<CloudServiceBinding> getAppBindings(UUID applicationGuid) {
        String uri = CloudControllerV3Endpoints.SERVICE_CREDENTIAL_BINDINGS + CloudControllerV3Endpoints.QUERY_APP_GUIDS + applicationGuid
            + CloudControllerV3Endpoints.AMPERSAND_PER_PAGE + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE;

        return listBindings(uri);
    }

    public List<CloudServiceBinding> getServiceBindingsForApplication(UUID applicationId, UUID serviceInstanceGuid) {
        String uri = CloudControllerV3Endpoints.SERVICE_CREDENTIAL_BINDINGS + CloudControllerV3Endpoints.QUERY_APP_GUIDS + applicationId
            + CloudControllerV3Endpoints.AMPERSAND_SERVICE_INSTANCE_GUIDS + serviceInstanceGuid
            + CloudControllerV3Endpoints.AMPERSAND_PER_PAGE
            + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE;

        return listBindings(uri);
    }

    public Map<String, Object> getServiceBindingParameters(UUID guid) {
        Map<String, Object> parameters = MiscUtil.cast(
            client.get(CloudControllerV3Endpoints.SERVICE_CREDENTIAL_BINDINGS + "/" + guid + "/parameters",
                       Map.class));

        if (parameters == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.PARAMETERS_OF_SERVICE_BINDING_WITH_GUID_0_NOT_FOUND,
                                                                   guid));
        }

        return parameters;
    }

    public void updateServiceBindingMetadata(UUID guid, Metadata metadata) {
        client.patch(CloudControllerV3Endpoints.SERVICE_CREDENTIAL_BINDING_BY_GUID,
                     Map.of(V3Fields.METADATA, OperationsUtil.toMetadataMap(metadata)), guid);
    }

    private List<CloudServiceBinding> listBindings(String uri) {
        return client.list(uri, BINDING_PAGE)
                     .stream()
                     .map(V3ServiceBindingMapper::toCloudServiceBinding)
                     .toList();
    }

    public List<String> unbindServiceInstance(UUID applicationGuid, UUID serviceInstanceGuid) {
        List<UUID> serviceBindingGuids = getServiceBindingGuids(applicationGuid, serviceInstanceGuid);

        if (serviceBindingGuids.isEmpty()) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND, MessageFormat.format(
                Messages.SERVICE_BINDING_BETWEEN_SERVICE_WITH_GUID_0_AND_APPLICATION_WITH_GUID_1_NOT_FOUND, serviceInstanceGuid,
                applicationGuid));
        }

        return deleteServiceBindings(serviceBindingGuids);
    }

    private List<UUID> getServiceBindingGuids(UUID applicationGuid, UUID serviceInstanceGuid) {
        String uri = CloudControllerV3Endpoints.SERVICE_CREDENTIAL_BINDINGS + CloudControllerV3Endpoints.QUERY_APP_GUIDS + applicationGuid
            + CloudControllerV3Endpoints.AMPERSAND_SERVICE_INSTANCE_GUIDS + serviceInstanceGuid
            + CloudControllerV3Endpoints.AMPERSAND_PER_PAGE
            + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE;

        return client.list(uri, BINDING_PAGE)
                     .stream()
                     .map(binding -> UUID.fromString(binding.guid()))
                     .toList();
    }

    private List<String> deleteServiceBindings(List<UUID> guids) {
        List<String> jobIds = new ArrayList<>();
        List<CloudOperationException> errors = new ArrayList<>();

        for (UUID guid : guids) {
            try {
                deleteServiceBinding(guid).ifPresent(jobIds::add);
            } catch (CloudOperationException e) {
                errors.add(e);
            }
        }

        OperationsUtil.throwOnErrors(errors);
        return jobIds;
    }

    public Optional<String> deleteServiceBinding(UUID guid) {
        ResponseEntity<Void> response = client.delete(CloudControllerV3Endpoints.SERVICE_CREDENTIAL_BINDING_BY_GUID, guid);

        return OperationsUtil.extractJobGuid(response);
    }

    private UUID getRequiredApplicationGuid(String applicationName) {
        String applicationsQuery = OperationsQueryUtil.buildApplicationsQuery(applicationName, targetSpace);

        List<V3GuidReference> apps = client.list(applicationsQuery, GUID_REFERENCE_PAGE);

        if (apps.isEmpty()) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.APPLICATION_0_NOT_FOUND, applicationName));
        }

        return UUID.fromString(apps.getFirst()
                                   .guid());
    }

    private UUID getRequiredServiceInstanceGuid(String name) {
        String serviceInstancesQuery = OperationsQueryUtil.buildServiceInstancesQuery(name, targetSpace);
        List<V3GuidReference> instances = client.list(serviceInstancesQuery, GUID_REFERENCE_PAGE);

        if (instances.isEmpty()) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.SERVICE_INSTANCE_0_NOT_FOUND, name));
        }

        return UUID.fromString(instances.getFirst()
                                        .guid());
    }

}
