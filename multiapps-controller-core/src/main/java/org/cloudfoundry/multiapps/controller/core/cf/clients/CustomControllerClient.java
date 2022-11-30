package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerHeaderConfiguration;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;

public abstract class CustomControllerClient {

    protected final CloudControllerClient client;
    private final WebClient webClient;
    private String correlationId = StringUtils.EMPTY;
    private CloudControllerHeaderConfiguration headerConfiguration;

    protected CustomControllerClient(CloudControllerClient client, String correlationID) {
        this.client = client;
        this.webClient = new WebClientFactory().getWebClient(client);
        this.correlationId = correlationID;
        this.headerConfiguration = new CloudControllerHeaderConfiguration();
    }

    protected CustomControllerClient(CloudControllerClient client) {
        this.client = client;
        this.webClient = new WebClientFactory().getWebClient(client);
        this.headerConfiguration = new CloudControllerHeaderConfiguration();
    }

    protected <T> List<T> getListOfResources(ResourcesResponseMapper<T> responseMapper, String uri, Object... urlVariables) {
        PaginationV3 pagination = addPageOfResources(uri, responseMapper, urlVariables);
        while (!StringUtils.isEmpty(pagination.getNextUri())) {
            pagination = addPageOfResources(pagination.getNextUri(), responseMapper);
        }
        return responseMapper.getMappedResources();
    }

    private PaginationV3 addPageOfResources(String uri, ResourcesResponseMapper<?> responseMapper, Object... urlVariables) {
        String responseString = webClient.get()
                                         .uri(uri, urlVariables)
                                         .headers(httpHeaders -> httpHeaders.addAll(generateRequestHeaders(correlationId)))
                                         .retrieve()
                                         .bodyToMono(String.class)
                                         .block();
        Map<String, Object> responseMap = JsonUtil.convertJsonToMap(responseString);
        responseMapper.addResources(responseMap);
        return PaginationV3.fromResponse(responseMap);
    }

    private MultiValueMap<String, String> generateRequestHeaders(String correlationId) {
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
