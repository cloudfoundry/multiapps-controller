package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.core.message.Messages;

public abstract class CloudServiceOperator extends CustomControllerClient {

    protected static final String SERVICE_INSTANCES_URL = "/v2/service_instances";
    protected static final String ACCEPTS_INCOMPLETE_TRUE = "?accepts_incomplete=true";
    protected static final String CREATE_SERVICE_URL_ACCEPTS_INCOMPLETE_TRUE = SERVICE_INSTANCES_URL + ACCEPTS_INCOMPLETE_TRUE;
    protected static final String SERVICE_NAME = "name";
    protected static final String SPACE_GUID = "space_guid";
    protected static final String SERVICE_PLAN_GUID = "service_plan_guid";
    protected static final String SERVICE_PARAMETERS = "parameters";
    protected static final String SERVICE_TAGS = "tags";

    protected CloudServiceOperator(RestTemplateFactory restTemplateFactory) {
        super(restTemplateFactory);
    }

    protected CloudServicePlan findPlanForService(CloudFoundryOperations client, CloudService service) {
        return findPlanForService(client, service, service.getPlan());
    }

    protected CloudServicePlan findPlanForService(CloudFoundryOperations client, CloudService service, String newPlan) {
        List<CloudServiceOffering> offerings = getServiceOfferings(client, service);
        for (CloudServiceOffering offering : offerings) {
            for (CloudServicePlan plan : offering.getCloudServicePlans()) {
                if (plan.getName()
                    .equals(newPlan)) {
                    return plan;
                }
            }
        }
        throw new CloudFoundryException(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.getReasonPhrase(),
            MessageFormat.format(Messages.NO_SERVICE_PLAN_FOUND, service.getName(), newPlan, service.getLabel()));
    }

    private List<CloudServiceOffering> getServiceOfferings(CloudFoundryOperations client, CloudService service) {
        List<CloudServiceOffering> offerings = client.getServiceOfferings();
        offerings = filterByLabel(offerings, service.getLabel());
        offerings = filterByVersion(offerings, service.getVersion());
        return offerings;
    }

    private List<CloudServiceOffering> filterByLabel(List<CloudServiceOffering> offerings, String label) {
        return offerings.stream()
            .filter(offering -> label.equals(offering.getLabel()))
            .collect(Collectors.toList());
    }

    protected List<CloudServiceOffering> filterByVersion(List<CloudServiceOffering> offerings, String version) {
        if (version == null) {
            return offerings;
        }
        return offerings.stream()
            .filter(offering -> version.equals(offering.getVersion()))
            .collect(Collectors.toList());
    }

}
