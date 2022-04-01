package org.cloudfoundry.multiapps.controller.core.cf.clients.v3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.client.v3.jobs.JobState;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.cloudfoundry.multiapps.controller.core.cf.clients.WebClientFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudEntity;

public abstract class CustomControllerClientV3 {

    private static final long SERVICE_KEY_CREATION_POLLING_PERIOD = TimeUnit.SECONDS.toMillis(5);
    private static final long JOB_POLLING_MAX_ATTEMPTS = 20;

    protected final CloudControllerClient client;
    private final WebClient webClient;

    protected CustomControllerClientV3(CloudControllerClient client) {
        this.client = client;
        this.webClient = new WebClientFactory().getWebClient(client);
    }

    protected UUID postResource(String resourceRequest, String uri, Object... urlVariables) {
        ResponseEntity<Void> response = webClient.post()
                                                 .uri(uri, urlVariables)
                                                 .contentType(MediaType.APPLICATION_JSON)
                                                 .bodyValue(resourceRequest)
                                                 .retrieve()
                                                 .toBodilessEntity()
                                                 .block();

        HttpStatus statusCode = response.getStatusCode();
        if (!statusCode.is2xxSuccessful()) {
            throw new SLException("Error during new service key creation: " + statusCode.toString() + " " + statusCode.getReasonPhrase());
        }
        List<String> locationHeader = response.getHeaders()
                                              .get("location");
        if (locationHeader == null || locationHeader.size() != 1) {
            throw new SLException("Cannot find location header in create service key response!");
        }

        Map<String, Object> keyCreationJobResponse = pollJob(locationHeader.get(0));

        LinksV3 links = LinksV3.fromResponse(keyCreationJobResponse);
        try {
            List<String> newKeyUriPath = links.getUriComponents("service_credential_binding")
                                              .getPathSegments();
            String newKeyGuid = newKeyUriPath.get(newKeyUriPath.size() - 1);

            return UUID.fromString(newKeyGuid);
        } catch (NullPointerException e) {
            throw new SLException("Cannot find the guid of the newly created service key in the uri: "
                + links.getUriString("service_credential_binding"));
        }
    }

    private Map<String, Object> pollJob(String jobUri) {
        int pollCount = 1;
        Map<String, Object> jobStatusMap = getJobStatus(jobUri);
        JobState jobState = JobState.from((String) jobStatusMap.get("state"));

        while (jobInProgress(jobState) && pollCount < JOB_POLLING_MAX_ATTEMPTS) {
            MiscUtil.sleep(SERVICE_KEY_CREATION_POLLING_PERIOD);
            ++pollCount;

            jobStatusMap = getJobStatus(jobUri);
            jobState = JobState.from((String) jobStatusMap.get("state"));
        }

        switch (jobState) {
            case COMPLETE:
                return jobStatusMap;
            case FAILED:
                throw new SLException("Service key creation failed! " + jobStatusMap.get("errors"));
            case POLLING:
            case PROCESSING:
            default:
                throw new SLException("Service key creation timeout!");
        }
    }

    private Map<String, Object> getJobStatus(String jobUri) {
        String responseString = webClient.get()
                                         .uri(jobUri)
                                         .retrieve()
                                         .bodyToMono(String.class)
                                         .block();
        return JsonUtil.convertJsonToMap(responseString);
    }

    private boolean jobInProgress(JobState state) {
        return state == JobState.POLLING || state == JobState.PROCESSING;
    }

    protected Map<String, Object> getResource(String uri, Object... urlVariables) {
        String responseString = webClient.get()
                                         .uri(uri, urlVariables)
                                         .retrieve()
                                         .bodyToMono(String.class)
                                         .block();
        Map<String, Object> responseMap = JsonUtil.convertJsonToMap(responseString);

        return responseMap;
    }

    protected <T extends CloudEntity> List<T> getListOfResources(ResourcesResponseMapper<T> responseMapper, String uri,
                                                                 Object... urlVariables) {
        PaginationV3 pagination = addPageOfResources(uri, responseMapper, urlVariables);
        while (!StringUtils.isEmpty(pagination.getNextUri())) {
            pagination = addPageOfResources(pagination.getNextUri(), responseMapper);
        }

        return responseMapper.getMappedResources();
    }

    private PaginationV3 addPageOfResources(String uri, ResourcesResponseMapper<?> responseMapper, Object... urlVariables) {
        String responseString = webClient.get()
                                         .uri(uri, urlVariables)
                                         .retrieve()
                                         .bodyToMono(String.class)
                                         .block();
        Map<String, Object> responseMap = JsonUtil.convertJsonToMap(responseString);
        responseMapper.addResources(responseMap);

        return PaginationV3.fromResponse(responseMap);
    }

    public abstract class ResourcesResponseMapper<T extends CloudEntity> {
        List<Map<String, Object>> queriedResources = new ArrayList<>();;
        Map<String, List<Object>> includedResources = new HashMap<>();

        @SuppressWarnings("unchecked")
        public void addResources(Map<String, Object> responseMap) {
            List<Map<String, Object>> newResources = (List<Map<String, Object>>) responseMap.get("resources");
            if (!CollectionUtils.isEmpty(newResources)) {
                queriedResources.addAll(newResources);
            }
            Map<String, List<Object>> included = (Map<String, List<Object>>) responseMap.get("included");
            if (included != null && !CollectionUtils.isEmpty(included.keySet())) {
                included.forEach((key, resources) -> includedResources.merge(key, resources, ListUtils::union));
            }
        }

        public List<Map<String, Object>> getQueriedResources() {
            return queriedResources;
        }

        public Map<String, List<Object>> getIncludedResources() {
            return includedResources;
        }

        public abstract List<T> getMappedResources();
    }
}
