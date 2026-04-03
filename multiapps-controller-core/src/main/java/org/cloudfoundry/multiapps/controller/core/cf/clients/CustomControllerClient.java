package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.facade.CloudCredentials;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerHeaderConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

public abstract class CustomControllerClient {

    protected static final int MAX_URI_QUERY_LENGTH = 4000;

    private final WebClient webClient;
    private String correlationId = StringUtils.EMPTY;
    private final CloudControllerHeaderConfiguration headerConfiguration;

    protected CustomControllerClient(ApplicationConfiguration configuration, WebClientFactory webClientFactory,
                                     CloudCredentials credentials, String correlationID) {
        this.webClient = webClientFactory.getWebClient(credentials);
        this.correlationId = correlationID;
        this.headerConfiguration = new CloudControllerHeaderConfiguration(configuration.getVersion());
    }

    protected CustomControllerClient(ApplicationConfiguration configuration, WebClientFactory webClientFactory,
                                     CloudCredentials credentials) {
        this.webClient = webClientFactory.getWebClient(credentials);
        this.headerConfiguration = new CloudControllerHeaderConfiguration(configuration.getVersion());
    }

    protected <T> List<T> getListOfResources(ResourcesResponseMapper<T> responseMapper, String uri) {
        PaginationV3 pagination = addPageOfResources(uri, responseMapper);
        while (!StringUtils.isEmpty(pagination.getNextUri())) {
            pagination = addPageOfResources(pagination.getNextUri(), responseMapper);
        }
        return responseMapper.getMappedResources();
    }

    protected <T> List<T> getListOfResourcesInBatches(ResourcesResponseMapper<T> responseMapper, String uriPrefix, String batchParamPrefix,
                                                      List<String> batchValues) {
        int fixedUriLength = uriPrefix.length() + batchParamPrefix.length();
        List<List<String>> batches = splitIntoBatches(batchValues, fixedUriLength);
        return batches.stream()
                      .map(batch -> {
                          String uri = uriPrefix + batchParamPrefix + String.join(",", batch);
                          return getListOfResources(responseMapper, uri);
                      })
                      .flatMap(List::stream)
                      .toList();
    }

    List<List<String>> splitIntoBatches(List<String> values, int fixedUriLength) {
        int maxBatchLength = Math.max(1, MAX_URI_QUERY_LENGTH - fixedUriLength);
        List<List<String>> batches = new ArrayList<>();
        List<String> currentBatch = new ArrayList<>();
        int currentLength = 0;

        for (String value : values) {
            // Account for the comma separator between values
            int addedLength = currentBatch.isEmpty() ? value.length() : value.length() + 1;
            if (!currentBatch.isEmpty() && currentLength + addedLength > maxBatchLength) {
                batches.add(currentBatch);
                currentBatch = new ArrayList<>();
                currentLength = 0;
                addedLength = value.length();
            }
            currentBatch.add(value);
            currentLength += addedLength;
        }

        if (!currentBatch.isEmpty()) {
            batches.add(currentBatch);
        }
        return batches;
    }

    private PaginationV3 addPageOfResources(String uri, ResourcesResponseMapper<?> responseMapper) {
        String responseString = webClient.get()
                                         .uri(uri)
                                         .headers(httpHeaders -> httpHeaders.addAll(generateRequestHeaders()))
                                         .retrieve()
                                         .bodyToMono(String.class)
                                         .block();
        Map<String, Object> responseMap = JsonUtil.convertJsonToMap(responseString);
        responseMapper.addResources(responseMap);
        return PaginationV3.fromResponse(responseMap);
    }

    private MultiValueMap<String, String> generateRequestHeaders() {
        var result = new LinkedMultiValueMap<String, String>();
        headerConfiguration.generateHeaders(correlationId)
                           .forEach(result::add);
        return result;
    }

    public static abstract class ResourcesResponseMapper<T> {
        List<Map<String, Object>> queriedResources = new ArrayList<>();
        Map<String, List<Object>> includedResources = new HashMap<>();

        @SuppressWarnings("unchecked")
        public void addResources(Map<String, Object> responseMap) {
            List<Map<String, Object>> newResources = (List<Map<String, Object>>) responseMap.get("resources");
            if (newResources != null) {
                queriedResources.addAll(newResources);
            }
            Map<String, List<Object>> included = (Map<String, List<Object>>) responseMap.get("included");
            if (included != null) {
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
