package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.sap.cloudfoundry.client.facade.CloudCredentials;
import com.sap.cloudfoundry.client.facade.domain.ImmutableInstanceInfo;
import com.sap.cloudfoundry.client.facade.domain.ImmutableInstancesInfo;
import com.sap.cloudfoundry.client.facade.domain.InstanceInfo;
import com.sap.cloudfoundry.client.facade.domain.InstanceState;
import com.sap.cloudfoundry.client.facade.domain.InstancesInfo;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.springframework.web.reactive.function.client.WebClient;

public class CustomInstancesInfoClient extends CustomControllerClient {

    private static final String GET_APPLICATION_PROCESS_URL = "/v3/apps/%s/processes/web/stats";

    public CustomInstancesInfoClient(WebClient webClient, String version) {
        super(webClient, version);
    }

    public CustomInstancesInfoClient(ApplicationConfiguration configuration,
                                     WebClientFactory webClientFactory, CloudCredentials credentials,
                                     String correlationID) {
        super(configuration, webClientFactory, credentials, correlationID);
    }

    public CustomInstancesInfoClient(ApplicationConfiguration configuration,
                                     WebClientFactory webClientFactory, CloudCredentials credentials) {
        super(configuration, webClientFactory, credentials);
    }

    public InstancesInfo getInstancesInfo(String appGuid) {
        return new CustomControllerClientErrorHandler().handleErrorsOrReturnResult(() -> doGetInstancesInfo(appGuid));
    }

    private InstancesInfo doGetInstancesInfo(String appGuid) {
        String url = String.format(GET_APPLICATION_PROCESS_URL, appGuid);
        var list = getListOfResources(new InstancesInfoResourceMapper(), url);
        return ImmutableInstancesInfo.builder()
                                     .instances(list)
                                     .build();
    }

    protected static class InstancesInfoResourceMapper extends ResourcesResponseMapper<InstanceInfo> {

        @Override
        public List<InstanceInfo> getMappedResources() {
            return getQueriedResources().stream()
                                        .map(this::buildInstancesInfo)
                                        .collect(Collectors.toList());
        }

        private InstanceInfo buildInstancesInfo(Map<String, Object> resource) {
            return ImmutableInstanceInfo.builder()
                                        .index(Integer.parseInt(resource.get("index")
                                                                        .toString()))
                                        .state(InstanceState.valueOf(resource.get("state")
                                                                             .toString()
                                                                             .toUpperCase()))
                                        .build();

        }
    }

}
