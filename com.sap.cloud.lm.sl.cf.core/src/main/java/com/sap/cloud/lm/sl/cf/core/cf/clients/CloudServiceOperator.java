package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.cloudfoundry.client.lib.util.CloudEntityResourceMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.cf.core.message.Messages;

public abstract class CloudServiceOperator extends CustomControllerClient {

    protected static final String SERVICES_URL = "/v2/services?inline-relations-depth=1";
    protected static final String SERVICE_INSTANCES_URL = "/v2/service_instances";
    protected static final String ACCEPTS_INCOMPLETE_TRUE = "?accepts_incomplete=true";
    protected static final String CREATE_SERVICE_URL_ACCEPTS_INCOMPLETE_TRUE = SERVICE_INSTANCES_URL + ACCEPTS_INCOMPLETE_TRUE;
    protected static final String SERVICE_NAME = "name";
    protected static final String SPACE_GUID = "space_guid";
    protected static final String SERVICE_PLAN_GUID = "service_plan_guid";
    protected static final String SERVICE_PARAMETERS = "parameters";
    protected static final String SERVICE_TAGS = "tags";

    protected CloudServicePlan findPlanForService(CloudService service, RestTemplate restTemplate, String cloudControllerUrl) {
        return findPlanForService(service, service.getPlan(), restTemplate, cloudControllerUrl);
    }

    protected CloudServicePlan findPlanForService(CloudService service, String newServicePlan, RestTemplate restTemplate,
        String cloudControllerUrl) {
        List<CloudServiceOffering> offerings = getServiceOfferings(service.getLabel(), restTemplate, cloudControllerUrl);
        offerings = filterByVersion(offerings, service);
        for (CloudServiceOffering offering : offerings) {
            for (CloudServicePlan plan : offering.getCloudServicePlans()) {
                if (plan.getName().equals(newServicePlan)) {
                    return plan;
                }
            }
        }
        throw new CloudFoundryException(HttpStatus.NOT_FOUND,
            MessageFormat.format(Messages.NO_SERVICE_PLAN_FOUND, service.getName(), newServicePlan, service.getLabel()));
    }

    protected List<CloudServiceOffering> filterByVersion(List<CloudServiceOffering> offerings, CloudService service) {
        if (service.getVersion() == null) {
            return offerings;
        }
        return offerings.stream().filter(offering -> service.getVersion().equals(offering.getVersion())).collect(Collectors.toList());
    }

    protected List<CloudServiceOffering> getServiceOfferings(String label, RestTemplate restTemplate, String cloudControllerUrl) {
        List<Map<String, Object>> resourceList = getAllResources(restTemplate, cloudControllerUrl, SERVICES_URL);
        List<CloudServiceOffering> results = new ArrayList<CloudServiceOffering>();
        for (Map<String, Object> resource : resourceList) {
            CloudServiceOffering cloudServiceOffering = getResourceMapper().mapResource(resource, CloudServiceOffering.class);
            if (cloudServiceOffering.getLabel().equals(label)) {
                results.add(cloudServiceOffering);
            }
        }
        return results;
    }

    protected CloudEntityResourceMapper getResourceMapper() {
        return new CloudEntityResourceMapper();
    }
}
