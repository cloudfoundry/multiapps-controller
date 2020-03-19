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

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.Messages;
import com.sap.cloud.lm.sl.cf.core.util.MethodExecution;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.common.SLException;

public class ServiceWithAlternativesCreator {

    private final UserMessageLogger userMessageLogger;

    public ServiceWithAlternativesCreator(UserMessageLogger userMessageLogger) {
        this.userMessageLogger = userMessageLogger;
    }

    public MethodExecution<String> createService(CloudControllerClient client, CloudServiceExtended service) {
        assertServiceAttributes(service);
        if (CollectionUtils.isEmpty(service.getAlternativeLabels())) {
            return createServiceInternal(client, service);
        }
        userMessageLogger.debug("Service \"{0}\" has defined service offering alternatives \"{1}\" for default service offering \"{2}\"",
                                service.getName(), service.getAlternativeLabels(), service.getLabel());
        List<String> possibleServiceOfferings = computePossibleServiceOfferings(service);
        List<CloudServiceOffering> serviceOfferings = client.getServiceOfferings();
        Map<String, List<CloudServicePlan>> existingServiceOfferings = serviceOfferings.stream()
                                                                                       .collect(Collectors.toMap(CloudServiceOffering::getName,
                                                                                                                 CloudServiceOffering::getServicePlans,
                                                                                                                 (v1,
                                                                                                                  v2) -> retrievePlanListFromServicePlan(service,
                                                                                                                                                         v1,
                                                                                                                                                         v2)));

        List<String> validServiceOfferings = computeValidServiceOfferings(possibleServiceOfferings, service.getPlan(),
                                                                          existingServiceOfferings);

        if (CollectionUtils.isEmpty(validServiceOfferings)) {
            throw new SLException(Messages.CANT_CREATE_SERVICE_NOT_MATCHING_OFFERINGS_OR_PLAN,
                                  service.getName(),
                                  possibleServiceOfferings,
                                  service.getPlan());
        }

        return attemptToFindServiceOfferingAndCreateService(client, service, validServiceOfferings);
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
                    .anyMatch(p -> servicePlanName.equalsIgnoreCase(p.getName()));
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
                                                                                 List<String> validServiceOfferings) {
        for (String validServiceOffering : validServiceOfferings) {
            try {
                CloudServiceExtended serviceWithCorrectLabel = ImmutableCloudServiceExtended.copyOf(service)
                                                                                            .withLabel(validServiceOffering);
                return createServiceInternal(client, serviceWithCorrectLabel);
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

    private MethodExecution<String> createServiceInternal(CloudControllerClient client, CloudServiceExtended service) {
        client.createService(service);
        return new MethodExecution<>(null, MethodExecution.ExecutionState.FINISHED);
    }

    private boolean shouldIgnoreException(CloudOperationException e) {
        return e.getStatusCode()
                .equals(HttpStatus.FORBIDDEN);
    }

    private void assertServiceAttributes(CloudServiceExtended service) {
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
