package com.sap.cloud.lm.sl.cf.core.helpers;

import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.cloudfoundry.client.lib.util.CloudEntityResourceMapper;
import org.cloudfoundry.client.lib.util.JsonUtil;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.CommonUtil;

public class ServiceCreator {

    @Inject
    protected RestTemplateFactory restTemplateFactory;

    private static final String SERVICES_URL = "/v2/services?inline-relations-depth=1";
    private static final String CREATE_SERVICE_URL = "/v2/service_instances?accepts_incomplete=false";
    private static final String SERVICE_NAME = "name";
    private static final String SPACE_GUID = "space_guid";
    private static final String SERVICE_PLAN_GUID = "service_plan_guid";
    private static final String SERVICE_PARAMETERS = "parameters";

    public void createService(CloudFoundryOperations client, CloudServiceExtended service, String spaceId) {
        assertServiceAttributes(service);

        RestTemplate restTemplate = restTemplateFactory.getRestTemplate(client);
        URL cloudControllerUrl = client.getCloudControllerUrl();
        CloudServicePlan cloudServicePlan = findPlanForService(service, restTemplate, cloudControllerUrl);

        Map<String, Object> serviceRequest = createServiceRequest(service, spaceId, cloudServicePlan);
        restTemplate.postForObject(getUrl(CREATE_SERVICE_URL, cloudControllerUrl), serviceRequest, String.class);
    }

    private Map<String, Object> createServiceRequest(CloudServiceExtended service, String spaceId, CloudServicePlan cloudServicePlan) {
        Map<String, Object> serviceRequest = new HashMap<String, Object>();
        serviceRequest.put(SPACE_GUID, spaceId);
        serviceRequest.put(SERVICE_NAME, service.getName());
        serviceRequest.put(SERVICE_PLAN_GUID, cloudServicePlan.getMeta().getGuid());
        serviceRequest.put(SERVICE_PARAMETERS, service.getCredentials());
        return serviceRequest;
    }

    private void assertServiceAttributes(CloudServiceExtended service) {
        assertNotNull(service, "Service must not be null");
        assertNotNull(service.getName(), "Service name must not be null");
        assertNotNull(service.getLabel(), "Service label must not be null");
        assertNotNull(service.getPlan(), "Service plan must not be null");
    }

    private void assertNotNull(Object serviceAttribute, String exceptionMessage) {
        if (serviceAttribute == null) {
            throw new SLException(exceptionMessage);
        }
    }

    private CloudServicePlan findPlanForService(CloudService service, RestTemplate restTemplate, URL cloudControllerUrl) {
        List<CloudServiceOffering> offerings = getServiceOfferings(service.getLabel(), restTemplate, cloudControllerUrl);
        offerings = filterByVersion(offerings, service);
        for (CloudServiceOffering offering : offerings) {
            for (CloudServicePlan plan : offering.getCloudServicePlans()) {
                if (service.getPlan() != null && service.getPlan().equals(plan.getName())) {
                    return plan;
                }
            }
        }
        throw new SLException(MessageFormat.format("Service plan {0} for service {1} not found", service.getPlan(), service.getName()));
    }

    private List<CloudServiceOffering> filterByVersion(List<CloudServiceOffering> offerings, CloudService service) {
        if (service.getVersion() == null) {
            return offerings;
        }
        return offerings.stream().filter(offering -> service.getVersion().equals(offering.getVersion())).collect(Collectors.toList());
    }

    private List<CloudServiceOffering> getServiceOfferings(String label, RestTemplate restTemplate, URL cloudControllerUrl) {
        List<Map<String, Object>> resourceList = getAllResources(restTemplate, cloudControllerUrl);
        List<CloudServiceOffering> results = new ArrayList<CloudServiceOffering>();
        for (Map<String, Object> resource : resourceList) {
            CloudServiceOffering cloudServiceOffering = getResourceMapper().mapResource(resource, CloudServiceOffering.class);
            if (cloudServiceOffering.getLabel() != null && label.equals(cloudServiceOffering.getLabel())) {
                results.add(cloudServiceOffering);
            }
        }
        return results;
    }

    private List<Map<String, Object>> getAllResources(RestTemplate restTemplate, URL cloudControllerUrl) {
        List<Map<String, Object>> allResources = new ArrayList<Map<String, Object>>();
        String resp = restTemplate.getForObject(getUrl(SERVICES_URL, cloudControllerUrl), String.class);
        Map<String, Object> respMap = JsonUtil.convertJsonToMap(resp);
        List<Map<String, Object>> newResources = CommonUtil.cast(respMap.get("resources"));
        if (newResources != null && newResources.size() > 0) {
            allResources.addAll(newResources);
        }
        String nextUrl = (String) respMap.get("next_url");
        while (nextUrl != null && nextUrl.length() > 0) {
            nextUrl = addPageOfResources(nextUrl, allResources, restTemplate, cloudControllerUrl);
        }
        return allResources;
    }

    @SuppressWarnings("unchecked")
    private String addPageOfResources(String nextUrl, List<Map<String, Object>> allResources, RestTemplate restTemplate,
        URL cloudControllerUrl) {
        String resp = restTemplate.getForObject(getUrl(nextUrl, cloudControllerUrl), String.class);
        Map<String, Object> respMap = JsonUtil.convertJsonToMap(resp);
        List<Map<String, Object>> newResources = (List<Map<String, Object>>) respMap.get("resources");
        if (newResources != null && newResources.size() > 0) {
            allResources.addAll(newResources);
        }
        return (String) respMap.get("next_url");
    }

    private String getUrl(String path, URL source) {
        return source + (path.startsWith("/") ? path : "/" + path);
    }

    protected CloudEntityResourceMapper getResourceMapper() {
        return new CloudEntityResourceMapper();
    }
}
