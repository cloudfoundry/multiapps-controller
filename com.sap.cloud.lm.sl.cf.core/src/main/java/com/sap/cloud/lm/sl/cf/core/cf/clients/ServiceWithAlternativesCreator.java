package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.exec.MethodExecution;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.common.SLException;

public class ServiceWithAlternativesCreator {

    private final ServiceCreator serviceCreator;
    private final UserMessageLogger userMessageLogger;

    public ServiceWithAlternativesCreator(ServiceCreator serviceCreator, UserMessageLogger userMessageLogger) {
        this.serviceCreator = serviceCreator;
        this.userMessageLogger = userMessageLogger;
    }

    public MethodExecution<String> createService(CloudControllerClient client, CloudServiceExtended service, String spaceId) {
        if (CollectionUtils.isEmpty(service.getAlternativeLabels())) {
            return serviceCreator.createService(client, service, spaceId);
        }
        userMessageLogger.debug("Service \"{0}\" has defined service offering alternatives \"{1}\" for default service offering \"{2}\"",
            service.getName(), service.getAlternativeLabels(), service.getLabel());
        List<String> possibleServiceOfferings = computePossibleServiceOfferings(service);
        List<CloudServiceOffering> serviceOfferings = client.getServiceOfferings();
        Map<String, List<CloudServicePlan>> existingServiceOfferings = serviceOfferings.stream()
            .collect(Collectors.toMap(CloudServiceOffering::getName, CloudServiceOffering::getCloudServicePlans,
                (v1, v2) -> retrievePlanListFromServicePlan(service, v1, v2)));

        List<String> validServiceOfferings = computeValidServiceOfferings(possibleServiceOfferings, service.getPlan(),
            existingServiceOfferings);

        if (CollectionUtils.isEmpty(validServiceOfferings)) {
            throw new SLException(Messages.CANT_CREATE_SERVICE_NOT_MATCHING_OFFERINGS_OR_PLAN, service.getName(), possibleServiceOfferings,
                service.getPlan());
        }

        return attemptToFindServiceOfferingAndCreateService(client, service, spaceId, validServiceOfferings);
    }

    private List<CloudServicePlan> retrievePlanListFromServicePlan(CloudServiceExtended service, List<CloudServicePlan>... listOfPlans) {
        String servicePlanName = service.getPlan();
        if (ArrayUtils.isEmpty(listOfPlans)) {
            throw new SLException(Messages.EMPTY_SERVICE_PLANS_LIST_FOUND, servicePlanName);
        }

        return Stream.of(listOfPlans)
            .filter(plans -> containsPlan(plans, servicePlanName))
            .findFirst()
            .orElse(listOfPlans[0]);
    }

    private boolean containsPlan(List<CloudServicePlan> plans, String servicePlanName) {
        return plans.stream()
            .filter(p -> servicePlanName.equalsIgnoreCase(p.getName()))
            .findFirst()
            .isPresent();
    }

    private List<String> computePossibleServiceOfferings(CloudServiceExtended service) {
        List<String> possibleServiceOfferings = new ArrayList<>(service.getAlternativeLabels());
        possibleServiceOfferings.add(0, service.getLabel());
        return possibleServiceOfferings;
    }

    private List<String> computeValidServiceOfferings(List<String> possibleServiceOfferings, String desiredServicePlan,
        Map<String, List<CloudServicePlan>> existingServiceOfferings) {
        List<String> validServiceOfferings = new ArrayList<>();
        for (String possibleServiceOffering : possibleServiceOfferings) {
            if (!existingServiceOfferings.containsKey(possibleServiceOffering)) {
                userMessageLogger.warnWithoutProgressMessage("Service offering \"{0}\" does not exist", possibleServiceOffering);
                continue;
            }
            Optional<CloudServicePlan> existingCloudServicePlan = existingServiceOfferings.get(possibleServiceOffering)
                .stream()
                .filter(servicePlan -> desiredServicePlan.equals(servicePlan.getName()))
                .findFirst();
            if (!existingCloudServicePlan.isPresent()) {
                userMessageLogger.warnWithoutProgressMessage("Service offering \"{0}\" does not provide service plan \"{1}\"",
                    possibleServiceOffering, desiredServicePlan);
                continue;
            }
            validServiceOfferings.add(possibleServiceOffering);
        }
        return validServiceOfferings;
    }

    private MethodExecution<String> attemptToFindServiceOfferingAndCreateService(CloudControllerClient client, CloudServiceExtended service,
        String spaceId, List<String> validServiceOfferings) {
        for (String validServiceOffering : validServiceOfferings) {
            try {
                service.setLabel(validServiceOffering);
                return serviceCreator.createService(client, service, spaceId);
            } catch (CloudOperationException e) {
                if (!shouldIgnoreException(e)) {
                    throw e;
                }
                userMessageLogger.warn("Service \"{0}\" creation with service offering \"{1}\" failed with \"{2}\"", service.getName(),
                    validServiceOffering, e.getMessage());
            }
        }
        throw new SLException(Messages.CANT_CREATE_SERVICE, service.getName(), validServiceOfferings);
    }

    private boolean shouldIgnoreException(CloudOperationException e) {
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

        public ServiceWithAlternativesCreator createInstance(UserMessageLogger userMessageLogger) {
            return new ServiceWithAlternativesCreator(serviceCreator, userMessageLogger);
        }

    }

}
