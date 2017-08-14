package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.cloudfoundry.client.lib.util.CloudEntityResourceMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.common.SLException;

public class ServiceCreator extends CustomControllerClient {

    private static final String SERVICES_URL = "/v2/services?inline-relations-depth=1";
    private static final String SERVICE_INSTANCES_URL = "/v2/service_instances";
    private static final String USER_PROVIDED_SERVICE_INSTANCES_URL = "/v2/user_provided_service_instances";
    private static final String ACCEPTS_INCOMPLETE_FALSE = "?accepts_incomplete=false";
    private static final String CREATE_SERVICE_URL_ACCEPTS_INCOMPLETE_FALSE = SERVICE_INSTANCES_URL + ACCEPTS_INCOMPLETE_FALSE;
    private static final String SERVICE_NAME = "name";
    private static final String SPACE_GUID = "space_guid";
    private static final String SERVICE_PLAN_GUID = "service_plan_guid";
    private static final String SERVICE_PARAMETERS = "parameters";
    private static final String SERVICE_CREDENTIALS = "credentials";

    public void createService(CloudFoundryOperations client, CloudServiceExtended service, String spaceId) {
        new CustomControllerClientErrorHandler().handleErrors(() -> attemptToCreateService(client, service, spaceId));
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
        return serviceRequest;
    }

    public void updateServiceParameters(CloudFoundryOperations client, String serviceName, Map<String, Object> parameters) {
        new CustomControllerClientErrorHandler().handleErrorsAndWarnings(
            () -> attemptToUpdateServiceParameters(client, serviceName, parameters));
    }

    private void attemptToUpdateServiceParameters(CloudFoundryOperations client, String serviceName, Map<String, Object> parameters) {
        assertServiceAttributes(serviceName, parameters);
        CloudService service = client.getService(serviceName);
        if (service == null) {
            throw new CloudFoundryException(HttpStatus.NOT_FOUND, "Not Found", "Service '" + serviceName + "' not found");
        }

        RestTemplate restTemplate = getRestTemplate(client);
        String cloudControllerUrl = client.getCloudControllerUrl().toString();

        if (service.isUserProvided()) {
            attemptToUpdateUserProvidedServiceCredentials(parameters, service, restTemplate, cloudControllerUrl);
            return;
        }

        attemptToUpdateServiceParameters(parameters, service, restTemplate, cloudControllerUrl);
    }

    private void attemptToUpdateServiceParameters(Map<String, Object> parameters, CloudService service, RestTemplate restTemplate,
        String cloudControllerUrl) {
        Map<String, Object> serviceRequest = createUpdateServiceParametersRequest(parameters);
        restTemplate.put(getUrl(cloudControllerUrl, getUpdateServiceParametersUrl(service.getMeta().getGuid().toString())), serviceRequest);
    }

    private Map<String, Object> createUpdateServiceParametersRequest(Map<String, Object> parameters) {
        Map<String, Object> updateServiceParametersRequest = new HashMap<String, Object>();
        updateServiceParametersRequest.put(SERVICE_PARAMETERS, parameters);
        return updateServiceParametersRequest;
    }

    private String getUpdateServiceParametersUrl(String serviceGuid) {
        return SERVICE_INSTANCES_URL + "/" + serviceGuid + ACCEPTS_INCOMPLETE_FALSE;
    }

    private void attemptToUpdateUserProvidedServiceCredentials(Map<String, Object> credentials, CloudService service,
        RestTemplate restTemplate, String cloudControllerUrl) {
        Map<String, Object> serviceRequest = createUpdateUserProvidedServiceCredentialsRequest(credentials);
        restTemplate.put(getUrl(cloudControllerUrl, getUpdateUserProvidedServiceCredentialsUrl(service.getMeta().getGuid().toString())),
            serviceRequest);
    }

    private Map<String, Object> createUpdateUserProvidedServiceCredentialsRequest(Map<String, Object> credentials) {
        Map<String, Object> updateServiceParametersRequest = new HashMap<String, Object>();
        updateServiceParametersRequest.put(SERVICE_CREDENTIALS, credentials);
        return updateServiceParametersRequest;
    }

    private String getUpdateUserProvidedServiceCredentialsUrl(String serviceGuid) {
        return USER_PROVIDED_SERVICE_INSTANCES_URL + "/" + serviceGuid + ACCEPTS_INCOMPLETE_FALSE;
    }

    private void assertServiceAttributes(CloudServiceExtended service) {
        assertNotNull(service, "Service must not be null");
        assertNotNull(service.getName(), "Service name must not be null");
        assertNotNull(service.getLabel(), "Service label must not be null");
        assertNotNull(service.getPlan(), "Service plan must not be null");
    }

    private void assertServiceAttributes(String serviceName, Map<String, Object> parameters) {
        assertNotNull(serviceName, "Service name must not be null");
        assertNotNull(parameters, "Service parameters must not be null");
    }

    private void assertNotNull(Object serviceAttribute, String exceptionMessage) {
        if (serviceAttribute == null) {
            throw new SLException(exceptionMessage);
        }
    }

    private CloudServicePlan findPlanForService(CloudService service, RestTemplate restTemplate, String cloudControllerUrl) {
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

    private List<CloudServiceOffering> getServiceOfferings(String label, RestTemplate restTemplate, String cloudControllerUrl) {
        List<Map<String, Object>> resourceList = getAllResources(restTemplate, cloudControllerUrl, SERVICES_URL);
        List<CloudServiceOffering> results = new ArrayList<CloudServiceOffering>();
        for (Map<String, Object> resource : resourceList) {
            CloudServiceOffering cloudServiceOffering = getResourceMapper().mapResource(resource, CloudServiceOffering.class);
            if (cloudServiceOffering.getLabel() != null && label.equals(cloudServiceOffering.getLabel())) {
                results.add(cloudServiceOffering);
            }
        }
        return results;
    }

    protected CloudEntityResourceMapper getResourceMapper() {
        return new CloudEntityResourceMapper();
    }
}
