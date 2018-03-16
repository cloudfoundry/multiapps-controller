package com.sap.cloud.lm.sl.cf.core.cf.clients;

import static java.text.MessageFormat.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.message.Messages;

public class ServiceWithAlternativesCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceWithAlternativesCreator.class);

    private final ServiceCreator serviceCreator;

    public ServiceWithAlternativesCreator(ServiceCreator serviceCreator) {
        this.serviceCreator = serviceCreator;
    }

    public void createService(CloudFoundryOperations client, CloudServiceExtended service, String spaceId) {
        if (CollectionUtils.isEmpty(service.getAlternativeLabels())) {
            serviceCreator.createService(client, service, spaceId);
            return;
        }
        LOGGER.debug(format("Service \"{0}\" has defined service offering alternatives \"{1}\" for default service offering \"{2}\"",
            service.getName(), service.getAlternativeLabels(), service.getLabel()));
        List<String> possibleServiceOfferings = computePossibleServiceOfferings(service);
        Map<String, List<CloudServicePlan>> existingServiceOfferings = client.getServiceOfferings()
            .stream()
            .collect(Collectors.toMap(CloudServiceOffering::getName, CloudServiceOffering::getCloudServicePlans));
        List<String> validServiceOfferings = computeValidServiceOfferings(possibleServiceOfferings, service.getPlan(),
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

    private List<String> computeValidServiceOfferings(List<String> possibleServiceOfferings, String desiredServicePlan,
        Map<String, List<CloudServicePlan>> existingServiceOfferings) {
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
                serviceCreator.createService(client, service, spaceId);
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

    private boolean shouldIgnoreException(CloudFoundryException e) {
        return e.getStatusCode()
            .equals(HttpStatus.FORBIDDEN);
    }

    @Component
    public static class Factory {

        private ServiceCreator serviceCreator;

        @Inject
        public Factory(ServiceCreator serviceCreator) {
            this.serviceCreator = serviceCreator;
        }

        public ServiceWithAlternativesCreator createInstance() {
            return new ServiceWithAlternativesCreator(serviceCreator);
        }

    }

}
