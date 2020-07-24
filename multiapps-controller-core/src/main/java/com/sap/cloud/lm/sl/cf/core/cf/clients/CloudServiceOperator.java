package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.core.Messages;

public abstract class CloudServiceOperator extends CustomControllerClient {

    protected static final String SERVICE_INSTANCES_URL = "/v2/service_instances";
    protected static final String SERVICE_NAME = "name";
    protected static final String SERVICE_PLAN_GUID = "service_plan_guid";
    protected static final String SERVICE_TAGS = "tags";

    protected CloudServiceOperator(RestTemplateFactory restTemplateFactory) {
        super(restTemplateFactory);
    }

    protected CloudServicePlan findPlanForService(CloudControllerClient client, CloudServiceInstance serviceInstance, String newPlan) {
        List<CloudServiceOffering> offerings = getServiceOfferings(client, serviceInstance);
        for (CloudServiceOffering offering : offerings) {
            for (CloudServicePlan plan : offering.getServicePlans()) {
                if (plan.getName()
                        .equals(newPlan)) {
                    return plan;
                }
            }
        }
        throw new CloudOperationException(HttpStatus.NOT_FOUND,
                                          HttpStatus.NOT_FOUND.getReasonPhrase(),
                                          MessageFormat.format(Messages.NO_SERVICE_PLAN_FOUND, serviceInstance.getName(), newPlan,
                                                               serviceInstance.getLabel()));
    }

    private List<CloudServiceOffering> getServiceOfferings(CloudControllerClient client, CloudServiceInstance service) {
        return client.getServiceOfferings()
                     .stream()
                     .filter(offering -> isSameLabel(offering, service.getLabel()))
                     .filter(offering -> isSameVersion(offering, service.getVersion()))
                     .collect(Collectors.toList());
    }

    private boolean isSameLabel(CloudServiceOffering offering, String label) {
        return label.equals(offering.getName());
    }

    private boolean isSameVersion(CloudServiceOffering offering, String version) {
        return version == null || version.equals(offering.getVersion());
    }

}
