package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceBroker;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServicePlanVisibility;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Fields;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3GuidReference;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceBroker;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceBrokerMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;

public class ServiceBrokersV3Operations {

    private static final ParameterizedTypeReference<V3ListResponse<V3ServiceBroker>> BROKER_PAGE = new ParameterizedTypeReference<>() {
    };

    private static final ParameterizedTypeReference<V3ListResponse<V3GuidReference>> GUID_REFERENCE_PAGE = new ParameterizedTypeReference<>() {
    };

    private final CloudControllerV3Client client;
    private final CloudSpace targetSpace;

    public ServiceBrokersV3Operations(CloudControllerV3Client client, CloudSpace targetSpace) {
        this.client = client;
        this.targetSpace = targetSpace;
    }

    public String createServiceBroker(CloudServiceBroker serviceBroker) {
        Assert.notNull(serviceBroker, "Service broker must not be null.");

        Map<String, Object> body = OperationsUtil.buildCreateServiceBrokerBody(serviceBroker);
        ResponseEntity<Void> response = client.post(CloudControllerV3Endpoints.SERVICE_BROKERS, body);

        return OperationsUtil.extractJobGuid(response)
                             .orElse(null);
    }

    public String deleteServiceBroker(String name) {
        CloudServiceBroker broker = getServiceBroker(name);
        UUID guid = broker.getMetadata()
                          .getGuid();

        ResponseEntity<Void> response = client.delete(CloudControllerV3Endpoints.SERVICE_BROKER_BY_GUID, guid.toString());

        return OperationsUtil.extractJobGuid(response)
                             .orElse(null);
    }

    public CloudServiceBroker getServiceBroker(String name) {
        return getServiceBroker(name, true);
    }

    public CloudServiceBroker getServiceBroker(String name, boolean required) {
        CloudServiceBroker serviceBroker = findServiceBrokerByName(name);

        if (serviceBroker == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.SERVICE_BROKER_0_NOT_FOUND, name));
        }

        return serviceBroker;
    }

    public List<CloudServiceBroker> getServiceBrokers() {
        return client.list(CloudControllerV3Endpoints.SERVICE_BROKERS + CloudControllerV3Endpoints.QUERY_PER_PAGE
                               + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE, BROKER_PAGE)
                     .stream()
                     .map(V3ServiceBrokerMapper::toCloudServiceBroker)
                     .toList();
    }

    public String updateServiceBroker(CloudServiceBroker serviceBroker) {
        Assert.notNull(serviceBroker, "Service broker must not be null.");

        CloudServiceBroker existingBroker = getServiceBroker(serviceBroker.getName());
        UUID brokerGuid = existingBroker.getMetadata()
                                        .getGuid();

        Map<String, Object> basicAuthentication = OperationsUtil.basicAuthentication(serviceBroker);
        Map<String, Object> body = Map.of(V3Fields.NAME, serviceBroker.getName(), V3Fields.URL, serviceBroker.getUrl(),
                                          V3Fields.AUTHENTICATION, basicAuthentication);

        ResponseEntity<Void> response = client.patch(CloudControllerV3Endpoints.SERVICE_BROKER_BY_GUID, body, brokerGuid.toString());

        return OperationsUtil.extractJobGuid(response)
                             .orElse(null);
    }

    public void updateServicePlanVisibilityForBroker(String name, ServicePlanVisibility visibility) {
        CloudServiceBroker broker = getServiceBroker(name);
        UUID brokerGuid = broker.getMetadata()
                                .getGuid();

        for (UUID servicePlanGuid : findServicePlanGuidsByBrokerGuid(brokerGuid)) {
            updateServicePlanVisibility(servicePlanGuid, visibility);
        }
    }

    private CloudServiceBroker findServiceBrokerByName(String name) {
        return client.list(
                         CloudControllerV3Endpoints.SERVICE_BROKERS + CloudControllerV3Endpoints.QUERY_NAMES + name
                             + CloudControllerV3Endpoints.AMPERSAND_PER_PAGE
                             + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE, BROKER_PAGE)
                     .stream()
                     .findFirst()
                     .map(V3ServiceBrokerMapper::toCloudServiceBroker)
                     .orElse(null);
    }

    private List<UUID> findServicePlanGuidsByBrokerGuid(UUID brokerGuid) {
        return findServiceOfferingGuidsByBrokerGuid(brokerGuid).stream()
                                                               .flatMap(offeringGuid -> findServicePlanGuidsByOfferingGuid(
                                                                   offeringGuid).stream())
                                                               .toList();
    }

    private List<UUID> findServiceOfferingGuidsByBrokerGuid(UUID brokerGuid) {
        String uri = CloudControllerV3Endpoints.SERVICE_OFFERINGS + CloudControllerV3Endpoints.QUERY_SERVICE_BROKER_GUIDS + brokerGuid
            + CloudControllerV3Endpoints.AMPERSAND_SPACE_GUIDS + OperationsUtil.getGuid(targetSpace)
            + CloudControllerV3Endpoints.AMPERSAND_PER_PAGE
            + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE;

        return client.list(uri, GUID_REFERENCE_PAGE)
                     .stream()
                     .map(V3GuidReference::guid)
                     .map(UUID::fromString)
                     .toList();
    }

    private List<UUID> findServicePlanGuidsByOfferingGuid(UUID serviceOfferingGuid) {
        String uri =
            CloudControllerV3Endpoints.SERVICE_PLANS + CloudControllerV3Endpoints.QUERY_SERVICE_OFFERING_GUIDS + serviceOfferingGuid
                + CloudControllerV3Endpoints.AMPERSAND_PER_PAGE + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE;

        return client.list(uri, GUID_REFERENCE_PAGE)
                     .stream()
                     .map(V3GuidReference::guid)
                     .map(UUID::fromString)
                     .toList();
    }

    private void updateServicePlanVisibility(UUID servicePlanGuid, ServicePlanVisibility visibility) {
        client.patch(CloudControllerV3Endpoints.SERVICE_PLAN_VISIBILITY, Map.of(V3Fields.TYPE, visibility.toString()),
                     servicePlanGuid.toString());
    }

}
