package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Named;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceInstanceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import com.sap.cloud.lm.sl.cf.core.Messages;
import com.sap.cloud.lm.sl.cf.core.util.MethodExecution;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.common.SLException;

public class ServiceWithAlternativesCreator {

    private final UserMessageLogger userMessageLogger;

    public ServiceWithAlternativesCreator(UserMessageLogger userMessageLogger) {
        this.userMessageLogger = userMessageLogger;
    }

    public MethodExecution<String> createService(CloudControllerClient client, CloudServiceInstanceExtended serviceInstance) {
        assertServiceAttributes(serviceInstance);
        if (CollectionUtils.isEmpty(serviceInstance.getAlternativeLabels())) {
            return createServiceInternal(client, serviceInstance);
        }
        userMessageLogger.debug("Service \"{0}\" has defined service offering alternatives \"{1}\" for default service offering \"{2}\"",
                                serviceInstance.getName(), serviceInstance.getAlternativeLabels(), serviceInstance.getLabel());
        List<String> possibleServiceOfferings = computePossibleServiceOfferings(serviceInstance);
        List<CloudServiceOffering> serviceOfferings = client.getServiceOfferings();
        Map<String, List<CloudServicePlan>> existingServiceOfferings = serviceOfferings.stream()
                                                                                       .collect(Collectors.toMap(CloudServiceOffering::getName,
                                                                                                                 CloudServiceOffering::getServicePlans,
                                                                                                                 (v1,
                                                                                                                  v2) -> retrievePlanListFromServicePlan(serviceInstance,
                                                                                                                                                         v1,
                                                                                                                                                         v2)));

        List<String> validServiceOfferings = computeValidServiceOfferings(possibleServiceOfferings, serviceInstance.getPlan(),
                                                                          existingServiceOfferings);

        if (CollectionUtils.isEmpty(validServiceOfferings)) {
            throw new SLException(Messages.CANT_CREATE_SERVICE_NOT_MATCHING_OFFERINGS_OR_PLAN,
                                  serviceInstance.getName(),
                                  possibleServiceOfferings,
                                  serviceInstance.getPlan());
        }

        return attemptToFindServiceOfferingAndCreateService(client, serviceInstance, validServiceOfferings);
    }

    private List<CloudServicePlan> retrievePlanListFromServicePlan(CloudServiceInstanceExtended serviceInstance,
                                                                   List<CloudServicePlan>... listOfPlans) {
        String servicePlanName = serviceInstance.getPlan();
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
                    .anyMatch(p -> servicePlanName.equalsIgnoreCase(p.getName()));
    }

    private List<String> computePossibleServiceOfferings(CloudServiceInstanceExtended serviceInstance) {
        List<String> possibleServiceOfferings = new ArrayList<>(serviceInstance.getAlternativeLabels());
        possibleServiceOfferings.add(0, serviceInstance.getLabel());
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

    private MethodExecution<String> attemptToFindServiceOfferingAndCreateService(CloudControllerClient client,
                                                                                 CloudServiceInstanceExtended serviceInstance,
                                                                                 List<String> validServiceOfferings) {
        for (String validServiceOffering : validServiceOfferings) {
            try {
                CloudServiceInstanceExtended serviceWithCorrectLabel = ImmutableCloudServiceInstanceExtended.copyOf(serviceInstance)
                                                                                                            .withLabel(validServiceOffering);
                return createServiceInternal(client, serviceWithCorrectLabel);
            } catch (CloudOperationException e) {
                if (!shouldIgnoreException(e)) {
                    throw e;
                }
                userMessageLogger.warn("Service \"{0}\" creation with service offering \"{1}\" failed with \"{2}\"",
                                       serviceInstance.getName(), validServiceOffering, e.getMessage());
            }
        }
        throw new SLException(Messages.CANT_CREATE_SERVICE, serviceInstance.getName(), validServiceOfferings);
    }

    private MethodExecution<String> createServiceInternal(CloudControllerClient client, CloudServiceInstanceExtended serviceInstance) {
        client.createServiceInstance(serviceInstance);
        return new MethodExecution<>(null, MethodExecution.ExecutionState.EXECUTING);
    }

    private boolean shouldIgnoreException(CloudOperationException e) {
        return e.getStatusCode()
                .equals(HttpStatus.FORBIDDEN);
    }

    private void assertServiceAttributes(CloudServiceInstanceExtended service) {
        Assert.notNull(service, "Service must not be null");
        Assert.notNull(service.getName(), "Service name must not be null");
        Assert.notNull(service.getLabel(), "Service label must not be null");
        Assert.notNull(service.getPlan(), "Service plan must not be null");
    }

    @Named
    public static class Factory {

        public ServiceWithAlternativesCreator createInstance(UserMessageLogger userMessageLogger) {
            return new ServiceWithAlternativesCreator(userMessageLogger);
        }

    }

}
