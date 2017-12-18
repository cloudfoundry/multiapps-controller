package com.sap.cloud.lm.sl.cf.core.cf.clients;

import static java.text.MessageFormat.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.message.Messages;

public class ServiceCreator extends CloudServiceOperator {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ServiceCreator.class);

    public void createService(CloudFoundryOperations client, CloudServiceExtended service, String spaceId) {
        if (CollectionUtils.isEmpty(service.getAlternativeLabels())) {
            createServiceInternal(client, service, spaceId);
            return;
        }
        LOGGER.debug(format("Service \"{0}\" has defined service offering alternatives \"{1}\" for default service offering \"{2}\"",
            service.getName(), service.getAlternativeLabels(), service.getLabel()));
        List<String> possibleServiceOfferings = computePossibleServiceOfferings(service);
        Map<String, List<CloudServicePlan>> existingServiceOfferings = client.getServiceOfferings().stream().collect(
            Collectors.toMap(CloudServiceOffering::getName, CloudServiceOffering::getCloudServicePlans));
        List<String> validServiceOfferings = computeValidServiceOfferings(client, possibleServiceOfferings, service.getPlan(),
            existingServiceOfferings);

        if (CollectionUtils.isEmpty(validServiceOfferings)) {
            LOGGER.error(format(
                "Service \"{0}\" could not be created because none of the service offering(s) \"{1}\" match with existing service offerings \"{2}\" or provide service plan \"{3}\"",
                service.getName(), possibleServiceOfferings, existingServiceOfferings.keySet(), service.getPlan()));
            throw new CloudFoundryException(HttpStatus.BAD_REQUEST, format(Messages.CANT_CREATE_SERVICE_NOT_MATCHING_OFFERINGS_OR_PLAN,
                service.getName(), possibleServiceOfferings, service.getPlan()));
        }

        attemptToFindServiceOfferingAndCreateService(client, service, spaceId, validServiceOfferings);
    }

    private List<String> computePossibleServiceOfferings(CloudServiceExtended service) {
        List<String> possibleServiceOfferings = new ArrayList<String>(service.getAlternativeLabels());
        possibleServiceOfferings.add(0, service.getLabel());
        return possibleServiceOfferings;
    }

    private List<String> computeValidServiceOfferings(CloudFoundryOperations client, List<String> possibleServiceOfferings,
        String desiredServicePlan, Map<String, List<CloudServicePlan>> existingServiceOfferings) {
        List<String> validServiceOfferings = new ArrayList<String>();
        for (String possibleServiceOffering : possibleServiceOfferings) {
            if (!existingServiceOfferings.containsKey(possibleServiceOffering)) {
                LOGGER.warn(format("Service offering \"{0}\" does not exist", possibleServiceOffering));
                continue;
            }
            Optional<CloudServicePlan> existingCloudServicePlan = existingServiceOfferings.get(possibleServiceOffering)
                .stream()
                .filter(servicePlan -> desiredServicePlan.equals(servicePlan.getName()))
                .findFirst();
            if (!existingCloudServicePlan.isPresent()) {
                LOGGER.warn(
                    format("Service offering \"{0}\" does not provide service plan \"{1}\"", possibleServiceOffering, desiredServicePlan));
                continue;
            }
            validServiceOfferings.add(possibleServiceOffering);
        }
        return validServiceOfferings;
    }

    private void attemptToFindServiceOfferingAndCreateService(CloudFoundryOperations client, CloudServiceExtended service, String spaceId,
        List<String> validServiceOfferings) {
        for (String validServiceOffering : validServiceOfferings) {
            try {
                service.setLabel(validServiceOffering);
                createServiceInternal(client, service, spaceId);
                return;
            } catch (CloudFoundryException e) {
                LOGGER.warn(format("Service \"{0}\" creation with service offering \"{1}\" failed with \"{2}\"", service.getName(),
                    validServiceOffering, e.getMessage()));
                if (!shouldIgnoreException(e)) {
                    throw e;
                }
            }
        }
        LOGGER.error(format("Service \"{0}\" could not be created because all atempt(s) to use service offerings \"{1}\" failed",
            service.getName(), validServiceOfferings));
        throw new CloudFoundryException(HttpStatus.BAD_REQUEST, format(Messages.CANT_CREATE_SERVICE, service.getName()));
    }

    protected void createServiceInternal(CloudFoundryOperations client, CloudServiceExtended service, String spaceId) {
        new CustomControllerClientErrorHandler().handleErrors(() -> attemptToCreateService(client, service, spaceId));
    }

    protected boolean shouldIgnoreException(CloudFoundryException ex) {
        return ex.getStatusCode().equals(HttpStatus.FORBIDDEN);
    }

    private void attemptToCreateService(CloudFoundryOperations client, CloudServiceExtended service, String spaceId) {
        assertServiceAttributes(service);

        RestTemplate restTemplate = getRestTemplate(client);
        String cloudControllerUrl = client.getCloudControllerUrl().toString();
        CloudServicePlan cloudServicePlan = findPlanForService(service, restTemplate, cloudControllerUrl);

        Map<String, Object> serviceRequest = createServiceRequest(service, spaceId, cloudServicePlan);
        restTemplate.postForObject(getUrl(cloudControllerUrl, CREATE_SERVICE_URL_ACCEPTS_INCOMPLETE_FALSE), serviceRequest, String.class);
    }

    private Map<String, Object> createServiceRequest(CloudServiceExtended service, String spaceId, CloudServicePlan cloudServicePlan) {
        Map<String, Object> serviceRequest = new HashMap<String, Object>();
        serviceRequest.put(SPACE_GUID, spaceId);
        serviceRequest.put(SERVICE_NAME, service.getName());
        serviceRequest.put(SERVICE_PLAN_GUID, cloudServicePlan.getMeta().getGuid());
        serviceRequest.put(SERVICE_PARAMETERS, service.getCredentials());
        serviceRequest.put(SERVICE_TAGS, service.getTags());
        return serviceRequest;
    }

    private void assertServiceAttributes(CloudServiceExtended service) {
        Assert.notNull(service, "Service must not be null");
        Assert.notNull(service.getName(), "Service name must not be null");
        Assert.notNull(service.getLabel(), "Service label must not be null");
        Assert.notNull(service.getPlan(), "Service plan must not be null");
    }
}
